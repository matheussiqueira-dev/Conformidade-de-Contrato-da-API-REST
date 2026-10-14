package com.swee.ordermanagementspring.api.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Endereco do contrato v4 (AddressRequest na entrada, Address na saida), usado por venda e cliente. */
public final class Addresses {
    private Addresses() {
    }

    public record AddressRequest(@NotBlank @Size(max = 120) String street,
                                 @NotBlank @Size(max = 10) String number,
                                 @Size(max = 60) String complement,
                                 @NotBlank @Size(max = 60) String neighborhood,
                                 @NotBlank @Size(max = 60) String city,
                                 @NotNull @Pattern(regexp = "^[A-Z]{2}$") String state,
                                 @NotNull @Pattern(regexp = "^[0-9]{8}$") String zipCode) { }

    public record Address(String street, String number, String complement, String neighborhood, String city,
                          String state, String zipCode) { }

    public static long insert(JdbcClient jdbc, AddressRequest a) {
        return jdbc.sql("""
                INSERT INTO address(street, number, complement, neighborhood, city, state, zip_code)
                VALUES (:street, :number, :complement, :neighborhood, :city, :state, :zip) RETURNING id""")
                .param("street", a.street()).param("number", a.number()).param("complement", a.complement())
                .param("neighborhood", a.neighborhood()).param("city", a.city()).param("state", a.state())
                .param("zip", a.zipCode()).query(Long.class).single();
    }

    public static void update(JdbcClient jdbc, long id, AddressRequest a) {
        jdbc.sql("""
                UPDATE address SET street = :street, number = :number, complement = :complement,
                       neighborhood = :neighborhood, city = :city, state = :state, zip_code = :zip WHERE id = :id""")
                .param("street", a.street()).param("number", a.number()).param("complement", a.complement())
                .param("neighborhood", a.neighborhood()).param("city", a.city()).param("state", a.state())
                .param("zip", a.zipCode()).param("id", id).update();
    }

    /** Le as colunas address_id, street, ..., zip_code de uma consulta com LEFT JOIN address. */
    public static Address read(ResultSet rs) throws SQLException {
        if (rs.getObject("address_id", Long.class) == null) return null;
        return new Address(rs.getString("street"), rs.getString("number"), rs.getString("complement"),
                rs.getString("neighborhood"), rs.getString("city"), rs.getString("state"), rs.getString("zip_code"));
    }
}
