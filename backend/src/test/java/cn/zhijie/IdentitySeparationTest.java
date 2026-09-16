package cn.zhijie;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.zhijie.config.SecurityConfig;
import cn.zhijie.dao.*;
import cn.zhijie.pojo.*;
import cn.zhijie.pojo.entity.*;
import cn.zhijie.pojo.query.*;
import cn.zhijie.pojo.request.*;
import cn.zhijie.security.*;
import cn.zhijie.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class IdentitySeparationTest {

    private AdminUserEntity admin(UUID id, String role, boolean enabled) {
        var user = new AdminUserEntity();
        user.setId(id);
        user.setUsername("same");
        user.setRole(role);
        user.setEnabled(enabled);
        user.setPermissionVersion(1);
        user.setPasswordHash(new BCryptPasswordEncoder().encode("password123"));
        return user;
    }

    @Test
    void sameUsernameAndIdRemainDistinctAndRefreshCannotCrossIdentity() {
        var customers = mock(UserMapper.class);
        var admins = mock(AdminUserMapper.class);
        UUID id = UUID.randomUUID();
        var manager = admin(id, "ADMIN", true);
        var customer = new UserEntity();
        customer.setId(id);
        customer.setUsername("same");
        customer.setRole("ADMIN");
        customer.setEnabled(true);
        customer.setPermissionVersion(1);
        customer.setPasswordHash(manager.getPasswordHash());
        when(customers.userByName("same")).thenReturn(customer);
        when(customers.user(id)).thenReturn(customer);
        when(admins.userByName("same")).thenReturn(manager);
        when(admins.user(id)).thenReturn(manager);
        try (var store = new MemorySessionStore(Clock.systemUTC())) {
            var auth = new AuthService(
                customers,
                admins,
                mock(Audit.class),
                store,
                store,
                new cn.zhijie.security.CaptchaService(
                    store,
                    store,
                    new cn.zhijie.config.CaptchaProperties(120, 2, 120, 5, 30, 60)
                ),
                "test-signing-key-for-identity-separation-12345"
            );
            var input = new LoginRequest("same", "password123");
            var userToken = auth.login(IdentityType.CUSTOMER, input, "local");
            var adminToken = auth.login(IdentityType.ADMIN, input, "local");
            var userActor = auth.authenticate(userToken.accessToken());
            var adminActor = auth.authenticate(adminToken.accessToken());
            assertEquals("USER", userActor.role());
            assertFalse(userActor.isAdmin());
            assertTrue(adminActor.isAdmin());
            assertThrows(ResponseStatusException.class, () -> userActor.require("ADMIN"));
            assertThrows(ResponseStatusException.class, adminActor::requireCustomer);
            assertThrows(ResponseStatusException.class, () ->
                auth.refresh(IdentityType.ADMIN, userToken.refreshToken())
            );
            assertThrows(ResponseStatusException.class, () ->
                auth.refresh(IdentityType.CUSTOMER, adminToken.refreshToken())
            );
            var refreshed = auth.refresh(IdentityType.ADMIN, adminToken.refreshToken());
            assertTrue(auth.authenticate(refreshed.accessToken()).isAdmin());
            manager.setPermissionVersion(2);
            assertThrows(ResponseStatusException.class, () ->
                auth.authenticate(refreshed.accessToken())
            );
            assertEquals(id, auth.authenticate(userToken.accessToken()).id());
        }
    }

    @Test
    void adminEditingLocksAllRowsAndPreservesLastAdmin() {
        var mapper = mock(AdminUserMapper.class);
        var service = new AdminAccountService(mapper, mock(Audit.class));
        UUID id = UUID.randomUUID();
        var row = admin(id, "ADMIN", true);
        var actor = new Actor(id, "ADMIN", "s", IdentityType.ADMIN);
        when(mapper.lockAccounts()).thenReturn(List.of(row));
        assertThrows(ResponseStatusException.class, () ->
            service.update(actor, id, new AdminUpdateRequest("same", "EDITOR", true))
        );
        assertThrows(ResponseStatusException.class, () ->
            service.update(actor, id, new AdminUpdateRequest("same", "ADMIN", false))
        );
        verify(mapper, never()).update(any());
        service.update(actor, id, new AdminUpdateRequest("renamed", "ADMIN", true));
        verify(mapper).update(
            argThat(command -> command.username().equals("renamed") && command.password() == null)
        );
        assertThrows(ResponseStatusException.class, () ->
            service.list(
                new Actor(UUID.randomUUID(), "EDITOR", "s", IdentityType.ADMIN),
                new AdminQuery()
            )
        );
    }

    @Test
    void attachmentAndAuditDistinguishSameUuidInDifferentTables() throws Exception {
        UUID id = UUID.randomUUID(), fileId = UUID.randomUUID();
        var mapper = mock(AttachmentMapper.class);
        var logs = mock(AuditMapper.class);
        var audit = new Audit(logs);
        var row = new AttachmentEntity();
        row.setId(fileId);
        row.setOwnerId(id);
        row.setAccessLevel("PRIVATE");
        when(mapper.attachment(fileId)).thenReturn(row);
        var service = new AttachmentService(mapper, audit, "target/test-attachments");
        var customer = new Actor(id, "USER", "s", IdentityType.CUSTOMER);
        var editor = new Actor(id, "EDITOR", "s", IdentityType.ADMIN);
        assertThrows(ResponseStatusException.class, () -> service.authorize(editor, fileId));
        assertEquals(fileId, service.authorize(customer, fileId).getId());
        verify(logs).audit(
            argThat(command -> id.equals(command.actor()) && command.adminActor() == null)
        );
        audit.log(editor, "ADMIN_ACTION", fileId, "测试");
        verify(logs).audit(
            argThat(command -> command.actor() == null && id.equals(command.adminActor()))
        );
    }

    @Test
    void sqlBootstrapUsesVerifiedBcryptAndCorrectRelations() throws Exception {
        String sql = Files.readString(Path.of("../deploy/sql/001_init.sql"));
        var match = java.util.regex.Pattern.compile(
            "'admin'\\s*,\\s*'([^']+)'\\s*,\\s*'ADMIN'"
        ).matcher(sql);
        assertTrue(match.find());
        assertTrue(new BCryptPasswordEncoder().matches("admin123", match.group(1)));
        assertTrue(sql.contains("reviewer_id uuid REFERENCES admin_user"));
        assertTrue(sql.contains("created_by uuid REFERENCES admin_user"));
        assertTrue(sql.replaceAll("\\s+", "").contains("num_nonnulls(owner_id,admin_owner_id)=1"));
        assertFalse(Files.exists(Path.of("../deploy/sql/002_bootstrap_admin.sql")));
    }

    @Test
    void passwordChangeTargetsOnlyAdminTableAndWritesNoPasswordToAudit() {
        UUID id = UUID.randomUUID();
        var admins = mock(AdminUserMapper.class);
        var customers = mock(UserMapper.class);
        var audit = mock(Audit.class);
        when(admins.lockUser(id)).thenReturn(admin(id, "ADMIN", true));
        var auth = new AuthService(
            customers,
            admins,
            audit,
            mock(SessionStore.class),
            mock(RateLimiter.class),
            mock(cn.zhijie.security.CaptchaService.class),
            "test-signing-key-for-identity-separation-12345"
        );
        var actor = new Actor(id, "ADMIN", "s", IdentityType.ADMIN);
        auth.updateAccount(actor, new AccountRequest("newname", "password123", "changed-password"));
        verify(admins).updateAccount(
            argThat(
                command ->
                    command.id().equals(id) &&
                    new BCryptPasswordEncoder().matches("changed-password", command.password())
            )
        );
        verifyNoInteractions(customers);
        verify(audit).log(
            eq(actor),
            eq("ACCOUNT_CHANGED"),
            eq(id),
            argThat(
                detail -> !detail.contains("password123") && !detail.contains("changed-password")
            )
        );
    }
}
