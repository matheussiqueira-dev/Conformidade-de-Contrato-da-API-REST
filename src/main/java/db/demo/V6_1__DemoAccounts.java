package db.demo;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * Contas de demonstracao do perfil demo (decisoes A8 e B-11). A senha vem de SBI_DEMO_PASSWORD e e
 * gravada so como BCrypt(12); sem a variavel, a migracao falha e o banco demo nao sobe.
 * Esta classe so e encontrada quando spring.flyway.locations inclui classpath:db/demo.
 */
public class V6_1__DemoAccounts extends BaseJavaMigration {

    static final String PASSWORD_VARIABLE = "SBI_DEMO_PASSWORD";
    static final int MIN_LENGTH = 12;
    static final int MAX_BCRYPT_BYTES = 72;

    record Account(String name, String email, String role, String dbRole) { }

    static final List<Account> ACCOUNTS = List.of(
            new Account("Rafael Mendes", "rafael@loja.test", "GERENTE", "gerente_rafael"),
            new Account("Ana Souza", "ana@loja.test", "VENDEDOR", "vendedora_ana"),
            new Account("Bruno Lima", "bruno@loja.test", "VENDEDOR", "vendedor_bruno"),
            new Account("Carla Alves", "carla@loja.test", "VENDEDOR", "vendedora_carla"));

    @Override
    public void migrate(Context context) throws Exception {
        String password = requirePassword(System.getenv(PASSWORD_VARIABLE));
        var encoder = new BCryptPasswordEncoder(12);
        String sql = "INSERT INTO app_users(name, email, password_hash, role, db_role) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insert = context.getConnection().prepareStatement(sql)) {
            for (Account account : ACCOUNTS) {
                insert.setString(1, account.name());
                insert.setString(2, account.email());
                insert.setString(3, encoder.encode(password));
                insert.setString(4, account.role());
                insert.setString(5, account.dbRole());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    static String requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("Perfil demo exige " + PASSWORD_VARIABLE + " definida no ambiente.");
        }
        if (password.length() < MIN_LENGTH) {
            throw new IllegalStateException(PASSWORD_VARIABLE + " precisa ter pelo menos " + MIN_LENGTH + " caracteres.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalStateException(PASSWORD_VARIABLE + " passa de " + MAX_BCRYPT_BYTES + " bytes (limite do BCrypt).");
        }
        return password;
    }
}
