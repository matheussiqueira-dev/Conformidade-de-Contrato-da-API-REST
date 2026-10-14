package com.swee.ordermanagementspring.controllers;

import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.exceptions.ErrorResponse;
import com.swee.ordermanagementspring.security.AppPrincipal;
import com.swee.ordermanagementspring.security.AreaResolver;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessions;
    private final AreaResolver areas;

    public AuthController(AuthenticationManager manager, SecurityContextRepository contexts,
                          SessionAuthenticationStrategy sessions, AreaResolver areas) {
        this.manager = manager;
        this.contexts = contexts;
        this.sessions = sessions;
        this.areas = areas;
    }
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(min = 8, max = 72) String password) {
        @AssertTrue(message = "Password must contain at most 72 UTF-8 bytes")
        public boolean isPasswordWithinBcryptLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }
    }
    /** Usuario da sessao com as areas liberadas pela hierarquia (contrato v4, REC-f). */
    public record UserSession(Long id, String name, String email, UserRole role, List<String> areas) {}
    public record LoginResponse(UserSession user) {}
    public record CsrfResponse(String headerName, String token) {}

    private UserSession session(AppPrincipal principal) {
        return new UserSession(principal.id(), principal.name(), principal.getUsername(), principal.role(),
                areas.areas(principal.getAuthorities()));
    }

    @GetMapping("/csrf")
    @Operation(summary = "Obtem token CSRF; renovar apos login/logout")
    public ResponseEntity<CsrfResponse> csrf(@io.swagger.v3.oas.annotations.Parameter(hidden = true) CsrfToken csrf) {
        return ResponseEntity.ok().header("Cache-Control", "no-store")
                .body(new CsrfResponse(csrf.getHeaderName(), csrf.getToken()));
    }
    @PostMapping("/login")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Sessao estabelecida"),
            @ApiResponse(responseCode = "400", description = "Credenciais malformadas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas, conta bloqueada ou inativa",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Token CSRF ausente ou invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest dto,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication auth = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.password()));
            // C7: principal inesperado e tratado como credencial invalida, sem corpo nulo
            if (!(auth.getPrincipal() instanceof AppPrincipal principal)) return unauthorized();
            sessions.onAuthentication(auth, request, response);
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, request, response);
            return ResponseEntity.ok().header("Cache-Control", "no-store").body(new LoginResponse(session(principal)));
        } catch (AuthenticationException ex) {
            return unauthorized();
        }
    }

    /** Mesmo corpo (salvo timestamp) para senha errada, conta inexistente, bloqueada ou inativa. */
    private static ResponseEntity<ErrorResponse> unauthorized() {
        return ResponseEntity.status(401).header("Cache-Control", "no-store").body(new ErrorResponse(401, "Unauthorized",
                ErrorCode.UNAUTHENTICATED, "Autenticacao necessaria ou credenciais invalidas"));
    }

    @GetMapping("/me")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Sessao ausente ou expirada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))})
    public ResponseEntity<UserSession> me(@AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(session(principal));
    }
}
