package db.demo;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A8: o perfil demo nao sobe sem senha valida das contas; nenhuma senha fica no repositorio. */
class DemoAccountsTest {

    @Test
    void missingOrShortPasswordStopsTheMigration() {
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword(null)).hasMessageContaining("SBI_DEMO_PASSWORD");
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword(" ")).hasMessageContaining("SBI_DEMO_PASSWORD");
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword("curta-7")).hasMessageContaining("8");
        assertThatThrownBy(() -> V6_1__DemoAccounts.requirePassword("é".repeat(37))).hasMessageContaining("72");
    }

    @Test
    void acceptedPasswordIsReturnedUnchangedAndAccountsMatchTheTeam() {
        assertThat(V6_1__DemoAccounts.requirePassword("senha-de-demo-local")).isEqualTo("senha-de-demo-local");
        assertThat(V6_1__DemoAccounts.ACCOUNTS).extracting(V6_1__DemoAccounts.Account::role)
                .containsExactly("GERENTE", "VENDEDOR", "VENDEDOR", "GERENTE");
        assertThat(V6_1__DemoAccounts.ACCOUNTS).extracting(V6_1__DemoAccounts.Account::email)
                .allMatch(email -> email.endsWith("@sbi.tech"));
    }

    @Test
    void ownPasswordWinsOverTheSharedOneAndIsValidatedToo() {
        var matheus = V6_1__DemoAccounts.ACCOUNTS.get(0);
        var allan = V6_1__DemoAccounts.ACCOUNTS.get(1);
        Map<String, String> env = Map.of("SBI_DEMO_PASSWORD", "senha-comum-local",
                "SBI_DEMO_PASSWORD_MATHEUS", "senha-propria");
        assertThat(V6_1__DemoAccounts.passwordFor(matheus, env::get)).isEqualTo("senha-propria");
        assertThat(V6_1__DemoAccounts.passwordFor(allan, env::get)).isEqualTo("senha-comum-local");
        assertThatThrownBy(() -> V6_1__DemoAccounts.passwordFor(allan, Map.of("SBI_DEMO_PASSWORD_ALLAN", "curta")::get))
                .hasMessageContaining("SBI_DEMO_PASSWORD_ALLAN");
        assertThatThrownBy(() -> V6_1__DemoAccounts.passwordFor(allan, Map.<String, String>of()::get))
                .hasMessageContaining("SBI_DEMO_PASSWORD");
    }
}
