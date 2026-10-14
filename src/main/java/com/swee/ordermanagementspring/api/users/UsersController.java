package com.swee.ordermanagementspring.api.users;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.users.UsersDtos.*;
import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.AppPrincipal;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Cadastro de usuarios pelo GERENTE (contrato v5, G-03). Nao existe DELETE: desativar substitui apagar. */
@RestController
@RequestMapping("/users")
public class UsersController {
    private final UsersService service;

    public UsersController(UsersService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<UserSummary> search(@RequestParam(defaultValue = "") @Size(max = 120) String q,
                                            @RequestParam(required = false) UserRole role,
                                            @RequestParam(required = false) Boolean active,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(new UsersRepository.Filter(q.trim(), role, active), page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UserDetail> create(@AuthenticationPrincipal AppPrincipal principal,
                                             @Valid @RequestBody UserCreateRequest request) {
        UserDetail created = service.create(CurrentUser.of(principal), request);
        return ResponseEntity.created(URI.create("/users/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public UserDetail get(@PathVariable @Positive long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public UserDetail update(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                             @Valid @RequestBody UserUpdateRequest request) {
        return service.update(CurrentUser.of(principal), id, request);
    }

    @PatchMapping("/{id}/role")
    public UserDetail changeRole(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                 @Valid @RequestBody UserRoleRequest request) {
        return service.changeRole(CurrentUser.of(principal), id, request);
    }

    @PatchMapping("/{id}/status")
    public UserDetail changeStatus(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                   @Valid @RequestBody UserStatusRequest request) {
        return service.changeStatus(CurrentUser.of(principal), id, request);
    }

    @PostMapping("/{id}/unlock")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Conta desbloqueada")
    public ResponseEntity<Void> unlock(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        service.unlock(CurrentUser.of(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/password-reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ApiResponse(responseCode = "204", description = "Senha temporaria definida")
    public ResponseEntity<Void> resetPassword(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                              @Valid @RequestBody PasswordResetRequest request) {
        service.resetPassword(CurrentUser.of(principal), id, request);
        return ResponseEntity.noContent().build();
    }
}
