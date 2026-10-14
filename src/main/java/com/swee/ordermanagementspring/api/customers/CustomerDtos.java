package com.swee.ordermanagementspring.api.customers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.swee.ordermanagementspring.api.common.Addresses.Address;
import com.swee.ordermanagementspring.api.common.Addresses.AddressRequest;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.Refs.NamedRef;
import com.swee.ordermanagementspring.entities.enums.Fulfillment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTOs de cliente do contrato v4. O documento entra so no cadastro e nunca muda pelo PUT (CT-CRM-104). */
public final class CustomerDtos {
    private CustomerDtos() {
    }

    static final String PHONE = "^[0-9()+\\-. ]{10,20}$";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = IndividualCustomerCreate.class, name = "INDIVIDUAL"),
            @JsonSubTypes.Type(value = CorporateCustomerCreate.class, name = "CORPORATE")})
    public sealed interface CustomerCreateRequest permits IndividualCustomerCreate, CorporateCustomerCreate {
        String name();
        String email();
        String phone();
        AddressRequest address();
    }

    public record IndividualCustomerCreate(@NotBlank @Size(min = 2, max = 120) String name,
                                           @NotNull @Pattern(regexp = "^\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}$") @CPF String cpf,
                                           @NotNull @Email @Size(max = 254) String email,
                                           @NotNull @Pattern(regexp = PHONE) String phone,
                                           @Past LocalDate birthDate,
                                           @Valid AddressRequest address) implements CustomerCreateRequest { }

    public record CorporateCustomerCreate(@NotBlank @Size(min = 2, max = 120) String name,
                                          @NotBlank @Size(min = 2, max = 120) String companyName,
                                          @NotNull @Pattern(regexp = "^\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}$") @CNPJ String cnpj,
                                          @NotNull @Email @Size(max = 254) String email,
                                          @NotNull @Pattern(regexp = PHONE) String phone,
                                          @Valid AddressRequest address) implements CustomerCreateRequest { }

    /** Campos opcionais ausentes (companyName, birthDate, address) mantem o valor atual (D-NOITE-09). */
    public record CustomerUpdateRequest(@NotBlank @Size(min = 2, max = 120) String name,
                                        @Size(min = 2, max = 120) String companyName,
                                        @NotNull @Email @Size(max = 254) String email,
                                        @NotNull @Pattern(regexp = PHONE) String phone,
                                        @Past LocalDate birthDate,
                                        @Valid AddressRequest address,
                                        @NotNull @Min(0) Integer version) { }

    public record CustomerSummary(long id, String type, String name, String document, String email, String phone,
                                  long ordersCount, OffsetDateTime lastOrderAt) { }

    public record CustomerDetail(long id, String type, String name, String companyName, String document, String email,
                                 String phone, LocalDate birthDate, Address address, OffsetDateTime createdAt,
                                 NamedRef createdBy, int version) { }

    public record CustomerOrderItem(String name, int quantity) { }

    /** DEC-ERP-02: total e vendedor so nos pedidos do proprio VENDEDOR (o GERENTE ve todos); nunca custo. */
    public record CustomerOrderSummary(long id, OffsetDateTime moment, String status, Fulfillment fulfillment,
                                       List<CustomerOrderItem> items,
                                       @JsonInclude(JsonInclude.Include.NON_NULL) MoneyDto total,
                                       @JsonInclude(JsonInclude.Include.NON_NULL) String sellerFirstName,
                                       boolean own) { }
}
