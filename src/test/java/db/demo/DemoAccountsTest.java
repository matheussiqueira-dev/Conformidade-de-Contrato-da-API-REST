package db.demo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A8: o perfil demo nao sobe sem SBI_DEMO_PASSWORD valida; nenhuma senha fica no repositorio. */
class DemoAccountsTest {

    @Test
    void missingOrShortPasswordStopsTheMigration() {
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword(null)).hasMessageContaining("SBI_DEMO_PASSWORD");
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword(" ")).hasMessageContaining("SBI_DEMO_PASSWORD");
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword("curta-11ch")).hasMessageContaining("12");
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword("é".repeat(37))).hasMessageContaining("72");
    }

    @Test
    void acceptedPasswordIsReturnedUnchangedAndAccountsCoverBothRoles() {
        assertThat(V6_1__DemoAccounts.requirePassword("senha-de-demo-local")).isEqualTo("senha-de-demo-local");
        assertThat(V6_1__DemoAccounts.ACCOUNTS).extracting(V6_1__DemoAccounts.Account::role)
                .containsExactly("GERENTE", "VENDEDOR", "VENDEDOR", "VENDEDOR");
    }
}
