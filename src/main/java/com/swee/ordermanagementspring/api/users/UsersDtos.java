package com.swee.ordermanagementspring.api.users;

import com.swee.ordermanagementspring.entities.auth.UserRole;
import com.swee.ordermanagementspring.security.StrongPassword;
import com.swee.ordermanagementspring.security.StrongPasswordValidator;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** Contrato v5 (G-03): nenhuma projecao traz senha ou hash. */
public final class UsersDtos {
    private UsersDtos() {}

    public record UserSummary(long id, String name, String email, UserRole role, boolean active,
                              OffsetDateTime lockedUntil, OffsetDateTime lastLoginAt) { }

    public record UserDetail(long id, String name, String email, UserRole role, boolean active,
                             OffsetDateTime lockedUntil, OffsetDateTime lastLoginAt, int failedLoginAttempts,
                             boolean mustChangePassword, int version, OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) { }

    public record UserCreateRequest(@NotBlank @Size(min = 3, max = 120) String name,
                                    @NotBlank @Email @Size(max = 254) String email,
                                    @NotNull UserRole role,
                                    @NotNull @StrongPassword String temporaryPassword) {
        @AssertTrue(message = "temporaryPassword must differ from the e-mail")
        public boolean isTemporaryPasswordDifferentFromEmail() {
            return !StrongPasswordValidator.sameAsEmail(temporaryPassword, email);
        }
    }

    public record UserUpdateRequest(@NotBlank @Size(min = 3, max = 120) String name,
                                    @NotBlank @Email @Size(max = 254) String email,
                                    @NotNull @Min(0) Integer version) { }

    public record UserRoleRequest(@NotNull UserRole role, @NotBlank @Size(min = 5, max = 200) String reason) { }

    public record UserStatusRequest(@NotNull Boolean active, @NotBlank @Size(min = 5, max = 200) String reason) { }

    public record PasswordResetRequest(@NotNull @StrongPassword String temporaryPassword) { }

    public record PasswordChangeRequest(@NotNull @Size(min = 1, max = 72) String currentPassword,
                                        @NotNull @StrongPassword String newPassword) { }
}
