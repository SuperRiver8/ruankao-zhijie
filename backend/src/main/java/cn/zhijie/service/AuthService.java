package cn.zhijie.service;

import static cn.zhijie.util.Support.*;

import cn.zhijie.dao.AdminUserMapper;
import cn.zhijie.dao.UserMapper;
import cn.zhijie.pojo.Actor;
import cn.zhijie.pojo.IdentityType;
import cn.zhijie.pojo.SessionRecord;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.pojo.response.*;
import cn.zhijie.security.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final AdminUserMapper admins;
    private final Audit audit;
    private final SessionStore sessions;
    private final RateLimiter limiter;
    private final byte[] secret;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(
        UserMapper userMapper,
        AdminUserMapper admins,
        Audit audit,
        SessionStore sessions,
        RateLimiter limiter,
        @Value("${app.jwt-secret}") String secret
    ) {
        this.userMapper = userMapper;
        this.admins = admins;
        this.audit = audit;
        this.sessions = sessions;
        this.limiter = limiter;
        this.secret = secret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        check(this.secret.length >= 32, "JWT_SECRET 至少 32 字节");
    }

    public TokenResponse register(LoginRequest p, String ip) {
        rate("register:" + ip, 10, 3600);
        String name = required(p.username(), "username"), password = required(
            p.password(),
            "password"
        );
        check(name.matches("[A-Za-z0-9_]{3,40}"), "用户名需为 3–40 位字母、数字或下划线");
        check(
            password.length() >= 10 &&
            password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
            "密码需至少 10 位且不超过 72 字节"
        );
        UUID id = UUID.randomUUID();
        userMapper.insertUser(new InsertUserCommand(id, name, encoder.encode(password)));
        return tokens(customer(userMapper.user(id)));
    }

    public TokenResponse login(IdentityType type, LoginRequest p, String ip) {
        rate(type + ":login:" + ip, 30, 300);
        Account u = type == IdentityType.ADMIN
            ? admin(admins.userByName(required(p.username(), "username")))
            : customer(userMapper.userByName(required(p.username(), "username")));
        String password = required(p.password(), "password");
        if (
            u == null ||
            !encoder.matches(password, u.passwordHash()) ||
            !Boolean.TRUE.equals(u.enabled())
        ) throw new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "用户名或密码错误，或账号不可用"
        );
        return tokens(u);
    }

    public void rate(String key, int limit, int seconds) {
        long n = limiter.increment(key, Duration.ofSeconds(seconds));
        if (n > limit) throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS,
            "请求过于频繁，请稍后再试"
        );
    }

    private String random() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private TokenResponse tokens(Account u) {
        String sid = UUID.randomUUID().toString(), refresh = random();
        sessions.create(
            sid,
            new SessionRecord(
                u.type(),
                u.id(),
                u.permissionVersion(),
                hash(refresh),
                Instant.now().plus(Duration.ofDays(7))
            ),
            Duration.ofDays(7)
        );
        return response(u, sid, refresh);
    }

    private TokenResponse response(Account u, String sid, String refresh) {
        try {
            SignedJWT jwt = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                new JWTClaimsSet.Builder()
                    .issuer("zhijie")
                    .claim("identityType", u.type().name())
                    .subject(u.id().toString())
                    .jwtID(sid)
                    .issueTime(new Date())
                    .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                    .build()
            );
            jwt.sign(new MACSigner(secret));
            return new TokenResponse(jwt.serialize(), sid + "." + refresh, 900);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public TokenResponse refresh(IdentityType type, String value) {
        if (value == null) throw unauthorized();
        String[] p = value.split("\\.");
        if (p.length != 2) throw unauthorized();
        SessionRecord session = sessions.find(p[0]).orElseThrow(this::unauthorized);
        if (session.identityType() != type) throw unauthorized();
        Account u = load(type, session.userId());
        if (
            u == null ||
            !Boolean.TRUE.equals(u.enabled()) ||
            !Objects.equals(session.userId(), u.id()) ||
            session.permissionVersion() != u.permissionVersion() ||
            session.identityType() != u.type()
        ) throw unauthorized();
        String next = random();
        if (
            !sessions.rotate(p[0], hash(p[1]), hash(next), Duration.ofDays(7))
        ) throw unauthorized();
        return response(u, p[0], next);
    }

    public Actor authenticate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (
                !JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) ||
                !jwt.verify(new MACVerifier(secret))
            ) throw unauthorized();
            JWTClaimsSet c = jwt.getJWTClaimsSet();
            if (
                !"zhijie".equals(c.getIssuer()) ||
                c.getExpirationTime() == null ||
                c.getExpirationTime().before(new Date())
            ) throw unauthorized();
            IdentityType type = IdentityType.valueOf(c.getStringClaim("identityType"));
            Account u = load(type, uuid(c.getSubject()));
            SessionRecord session = sessions.find(c.getJWTID()).orElseThrow(this::unauthorized);
            if (
                u == null ||
                !Boolean.TRUE.equals(u.enabled()) ||
                !Objects.equals(session.userId(), u.id()) ||
                session.permissionVersion() != u.permissionVersion() ||
                session.identityType() != u.type()
            ) throw unauthorized();
            return new Actor(u.id(), u.role(), c.getJWTID(), u.type());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    public void logout(Actor a) {
        sessions.revoke(a.session());
    }

    @org.springframework.transaction.annotation.Transactional
    public void updateAccount(Actor actor, AccountRequest request) {
        Account user = actor.isAdmin()
            ? admin(admins.lockUser(actor.id()))
            : customer(userMapper.lockUser(actor.id()));
        found(user);
        check(Boolean.TRUE.equals(user.enabled()), "账户已禁用");
        check(encoder.matches(request.currentPassword(), user.passwordHash()), "当前密码不正确");
        check(
            request.newPassword().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72,
            "密码不能超过72字节"
        );
        var command = new UpdateAccountCommand(
            actor.id(),
            request.username(),
            encoder.encode(request.newPassword())
        );
        if (actor.isAdmin()) admins.updateAccount(command);
        else userMapper.updateAccount(command);
        audit.log(actor, "ACCOUNT_CHANGED", actor.id(), "修改本人账号及密码");
    }

    private record Account(
        UUID id,
        String passwordHash,
        String role,
        Boolean enabled,
        int permissionVersion,
        IdentityType type
    ) {}

    private Account customer(UserEntity row) {
        return row == null
            ? null
            : new Account(
                row.getId(),
                row.getPasswordHash(),
                "USER",
                row.getEnabled(),
                row.getPermissionVersion(),
                IdentityType.CUSTOMER
            );
    }

    private Account admin(AdminUserEntity row) {
        return row == null
            ? null
            : new Account(
                row.getId(),
                row.getPasswordHash(),
                row.getRole(),
                row.getEnabled(),
                row.getPermissionVersion(),
                IdentityType.ADMIN
            );
    }

    private Account load(IdentityType type, UUID id) {
        return type == IdentityType.ADMIN ? admin(admins.user(id)) : customer(userMapper.user(id));
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录");
    }
}
