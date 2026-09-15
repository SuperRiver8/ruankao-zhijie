package cn.zhijie.security;

import cn.zhijie.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService auth;
    private final SecurityErrorHandler errors;

    public JwtAuthenticationFilter(AuthService auth, SecurityErrorHandler errors) {
        this.auth = auth;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                var actor = auth.authenticate(header.substring(7));
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            actor,
                            null,
                            List.of(
                                new SimpleGrantedAuthority("ROLE_" + actor.role()),
                                new SimpleGrantedAuthority("IDENTITY_" + actor.identityType())
                            )
                        )
                    );
            } catch (ResponseStatusException error) {
                SecurityContextHolder.clearContext();
                errors.write(
                    response,
                    error.getStatusCode().value(),
                    error.getStatusCode().value() == 503 ? "SERVICE_UNAVAILABLE" : "UNAUTHORIZED",
                    error.getReason() == null ? "登录已失效" : error.getReason()
                );
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
