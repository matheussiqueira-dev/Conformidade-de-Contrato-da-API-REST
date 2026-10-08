package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.ClientRequestDTO;
import com.swee.ordermanagementspring.dto.ProductRequestDTO;
import com.swee.ordermanagementspring.entities.client.Client;
import com.swee.ordermanagementspring.entities.client.CorporateClient;
import com.swee.ordermanagementspring.entities.client.IndividualClient;
import com.swee.ordermanagementspring.entities.product.DigitalProduct;
import com.swee.ordermanagementspring.entities.product.PhysicalProduct;
import com.swee.ordermanagementspring.entities.product.Product;
import com.swee.ordermanagementspring.exceptions.ProductException;
import com.swee.ordermanagementspring.exceptions.ResourceNotFoundException;
import com.swee.ordermanagementspring.repositories.AddressRepository;
import com.swee.ordermanagementspring.repositories.ClientRepository;
import com.swee.ordermanagementspring.repositories.ProductRepository;
import com.swee.ordermanagementspring.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tabelas de decisao de cadastro (RF-03, RF-04, RF-10): tipo x campo obrigatorio. */
class CatalogServicesTest {

    @Nested
    @DisplayName("ProductService")
    class Products {
        private ProductRepository repository;
        private ProductService service;

        @BeforeEach
        void setUp() {
            repository = mock(ProductRepository.class);
            when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
            service = new ProductService(repository);
        }

        @ParameterizedTest(name = "[CT-PROD-DT] tipo={0} peso={1} link={2} -> {3}")
        @CsvSource(nullValues = "null", value = {
                "PHYSICAL, 1.5,  null,                       PHYSICAL",
                "physical, 0.0,  null,                       PHYSICAL",
                "PHYSICAL, null, null,                       ERRO",
                "DIGITAL,  null, https://example.test/e-book, DIGITAL",
                "DIGITAL,  null, '   ',                      ERRO",
                "DIGITAL,  null, null,                       ERRO",
                "BOOK,     1.0,  https://example.test/e-book, ERRO"
        })
        void decisionTable(String type, Double weight, String link, String expected) {
            ProductRequestDTO dto = TestData.physicalProduct(10.0, weight);
            dto.setType(type);
            dto.setDownloadLink(link);
            if ("ERRO".equals(expected)) {
                assertThatThrownBy(() -> service.insert(dto)).isInstanceOf(ProductException.class);
                verify(repository, never()).save(any());
            } else {
                Product saved = service.insert(dto);
                assertThat(saved).isInstanceOf("PHYSICAL".equals(expected) ? PhysicalProduct.class : DigitalProduct.class);
            }
        }

        @Test
        @DisplayName("[CT-PROD-UPD] atualizacao mantem peso quando nao informado")
        void updateKeepsWeightWhenAbsent() {
            PhysicalProduct existing = new PhysicalProduct(10.0, "Antigo", "Fixture", 3.0);
            when(repository.findById(1L)).thenReturn(Optional.of(existing));

            service.update(1L, TestData.physicalProduct(12.5, null));

            assertThat(existing.getName()).isEqualTo("Mouse");
            assertThat(existing.getPrice()).isEqualTo(12.5);
            assertThat(existing.getWeight()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("[CT-PROD-404] produto inexistente retorna nao encontrado")
        void missingProduct() {
            when(repository.findById(9L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(9L)).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @Tag("known-defect")
        @DisplayName("[D011] excluir produto inexistente retorna nao encontrado")
        void deletingMissingProductFails() {
            when(repository.existsById(9L)).thenReturn(false);
            assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("ClientService")
    class Clients {
        private ClientRepository repository;
        private ClientService service;

        @BeforeEach
        void setUp() {
            repository = mock(ClientRepository.class);
            when(repository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
            service = new ClientService(repository);
        }

        @Test
        @DisplayName("[CA-03-03] pessoa fisica com CPF e criada")
        void individualWithCpf() {
            assertThat(service.insert(TestData.individual("12345678901"))).isInstanceOf(IndividualClient.class);
        }

        @Test
        @DisplayName("[CA-03-04] pessoa juridica com CNPJ e criada com razao social")
        void corporateWithCnpj() {
            Client saved = service.insert(TestData.corporate("12345678000199"));
            assertThat(saved).isInstanceOfSatisfying(CorporateClient.class,
                    c -> assertThat(c.getCompanyName()).isEqualTo("Loja Sintetica LTDA"));
        }

        @ParameterizedTest(name = "[CA-03-01/02] documento obrigatorio ausente para {0} -> erro de cliente")
        @ValueSource(strings = {"INDIVIDUAL", "CORPORATE"})
        @Tag("known-defect")
        void missingDocumentIsAClientError(String type) {
            ClientRequestDTO dto = "INDIVIDUAL".equals(type) ? TestData.individual(" ") : TestData.corporate(null);
            // [D008] erro de cliente nao pode ser classificado como erro de produto.
            assertThatThrownBy(() -> service.insert(dto))
                    .isInstanceOf(RuntimeException.class)
                    .isNotInstanceOf(ProductException.class)
                    .hasMessageContaining("INDIVIDUAL".equals(type) ? "cpf" : "cnpj");
        }

        @Test
        @DisplayName("[CT-CLI-005] tipo de cliente invalido e rejeitado")
        void invalidType() {
            ClientRequestDTO dto = TestData.individual("12345678901");
            dto.setType("GOVERNO");
            assertThatThrownBy(() -> service.insert(dto)).hasMessageContaining("GOVERNO");
        }

        @Test
        @DisplayName("[CT-CLI-006] atualizacao de pessoa juridica troca CNPJ e razao social")
        void corporateUpdate() {
            CorporateClient existing = new CorporateClient("Antigo", "old@example.test", LocalDate.of(1990, 1, 1), "1", "Old");
            when(repository.findById(3L)).thenReturn(Optional.of(existing));

            service.update(3L, TestData.corporate("12345678000199"));

            assertThat(existing.getCnpj()).isEqualTo("12345678000199");
            assertThat(existing.getCompanyName()).isEqualTo("Loja Sintetica LTDA");
            assertThat(existing.getEmail()).isEqualTo("marina@example.test");
        }

        @Test
        @DisplayName("[CT-CLI-007] atualizacao de pessoa fisica sem CPF preserva o CPF")
        void individualUpdateKeepsCpf() {
            IndividualClient existing = TestData.client();
            ReflectionTestUtils.setField(existing, "id", 4L);
            when(repository.findById(4L)).thenReturn(Optional.of(existing));

            service.update(4L, TestData.individual(null));

            assertThat(existing.getCpf()).isEqualTo("12345678901");
        }

        @Test
        @Tag("known-defect")
        @DisplayName("[D011] excluir cliente inexistente retorna nao encontrado")
        void deletingMissingClientFails() {
            when(repository.existsById(9L)).thenReturn(false);
            assertThatThrownBy(() -> service.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("AddressService")
    class Addresses {
        @Mock
        private AddressRepository repository;

        @InjectMocks
        private AddressService service;

        @Test
        @DisplayName("[CT-ADDR-002] endereco inexistente retorna nao encontrado")
        void missingAddress() {
            when(repository.findById(8L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(8L, TestData.address())).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @Tag("known-defect")
        @DisplayName("[D011] excluir endereco inexistente retorna nao encontrado")
        void deletingMissingAddressFails() {
            when(repository.existsById(8L)).thenReturn(false);
            assertThatThrownBy(() -> service.delete(8L)).isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
