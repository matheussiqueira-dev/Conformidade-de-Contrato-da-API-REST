package db.demo;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Contas da equipe no perfil demo (decisoes A8 e B-11). Cada conta usa a senha da sua variavel
 * (SBI_DEMO_PASSWORD_MATHEUS etc.) ou, sem ela, a comum SBI_DEMO_PASSWORD; sem nenhuma das duas, a migracao
 * falha e o banco demo nao sobe. A senha e gravada so como BCrypt(12) e nunca fica no repositorio.
 * Esta classe so e encontrada quando spring.flyway.locations inclui classpath:db/demo.
 */
public class V6_1__DemoAccounts extends BaseJavaMigration {

    static final String PASSWORD_VARIABLE = "SBI_DEMO_PASSWORD";
    static final int MIN_LENGTH = 8;
    static final int MAX_BCRYPT_BYTES = 72;

    record Account(String name, String email, String role, String dbRole, String passwordVariable) { }

    static final List<Account> ACCOUNTS = List.of(
            new Account("Matheus Siqueira", "matheussiqueira@sbi.tech", "GERENTE", "gerente_matheus",
                    PASSWORD_VARIABLE + "_MATHEUS"),
            new Account("Allan Barros", "allanbarros@sbi.tech", "VENDEDOR", "vendedor_allan",
                    PASSWORD_VARIABLE + "_ALLAN"),
            new Account("Francisco Ferreira", "franciscoferreira@sbi.tech", "VENDEDOR", "vendedor_francisco",
                    PASSWORD_VARIABLE + "_FRANCISCO"),
            new Account("Gabriel Sousa", "gabrielsousa@sbi.tech", "GERENTE", "gerente_gabriel",
                    PASSWORD_VARIABLE + "_GABRIEL"));

    @Override
    public void migrate(Context context) throws Exception {
        var encoder = new BCryptPasswordEncoder(12);
        String sql = "INSERT INTO app_users(name, email, password_hash, role, db_role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insert = context.getConnection().prepareStatement(sql)) {
            for (Account account : ACCOUNTS) {
                insert.setString(1, account.name());
                insert.setString(2, account.email());
                insert.setString(3, encoder.encode(passwordFor(account, System::getenv)));
                insert.setString(4, account.role());
                insert.setString(5, account.dbRole());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    /** Senha propria da conta, se definida; senao a comum. Valida o que for usado. */
    static String passwordFor(Account account, UnaryOperator<String> env) {
        String own = env.apply(account.passwordVariable());
        if (own != null && !own.isBlank()) {
            return requirePassword(own, account.passwordVariable());
        }
        return requirePassword(env.apply(PASSWORD_VARIABLE), PASSWORD_VARIABLE);
    }

    static String requirePassword(String password) {
        return requirePassword(password, PASSWORD_VARIABLE);
    }

    private static String requirePassword(String password, String variable) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Perfil demo exige " + variable + " definida no ambiente.");
        }
        if (password.length() < MIN_LENGTH) {
            throw new IllegalStateException(variable + " precisa ter pelo menos " + MIN_LENGTH + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalStateException(variable + " passa de " + MAX_BCRYPT_BYTES + " bytes (limite do BCrypt).");
        }
        return password;
    }
}
