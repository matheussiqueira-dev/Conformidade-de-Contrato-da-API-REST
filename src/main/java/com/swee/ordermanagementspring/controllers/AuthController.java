package com.swee.ordermanagementspring.controllers;

import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AppPrincipal;
import com.swee.ordermanagementspring.security.SecurityJson;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;

    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts,
                          SessionAuthenticationStrategy sessions) {
        this.manager = manager;
        this.contexts = contexts;
        this.sessions = sessions;
    }
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(min = 8, max = 72) String password) {}
    public record UserSession(Long id, String name, String email, UserRole role) {
        static UserSession from(AppPrincipal principal) {
            return new UserSession(principal.id(), principal.name(), principal.getUsername(), principal.role());
        }
    }
    public record LoginResponse(UserSession user) {}
    public record CsrfResponse(String headerName, String token) {}

    @GetMapping("/csrf")
    public ResponseEntity<CsrfResponse> csrf(CsrfToken csrf) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new CsrfResponse(csrf.getHeaderName(), csrf.getToken()));
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest dto,
                                              HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            var auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));
            sessions.onAuthentication(auth, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            return ResponseEntity.ok().header("Cache-Control", "no-store")
                    .body(new LoginResponse(UserSession.from((AppPrincipal) auth.getPrincipal())));
        } catch (AuthenticationException ex) {
            SecurityJson.error(response, 401);
            return null;
        }
    }
    @GetMapping("/me")
    public ResponseEntity<UserSession> me(@AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(UserSession.from(principal));
    }
}
