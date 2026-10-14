package com.swee.ordermanagementspring.api.backoffice;

import com.swee.ordermanagementspring.api.backoffice.BackofficeDtos.*;
import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.exceptions.ErrorResponse;
import com.swee.ordermanagementspring.security.AppPrincipal;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import java.util.List;

/** Produtos e categorias do contrato v4 (substitui o /products legado). Somente GERENTE (URL e servico). */
@RestController
public class ProductsController {
    private final CatalogAdminService service;

    public ProductsController(CatalogAdminService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public PageResponse<Product> list(@RequestParam(defaultValue = "") @Size(max = 120) String q,
                                      @RequestParam(required = false) Boolean sellable,
                                      @RequestParam(required = false) Boolean active,
                                      @RequestParam(defaultValue = "0") @Min(0) int page,
                                      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(q, sellable, active, page, size);
    }

    @PostMapping("/products")
    @ApiResponse(responseCode = "400", description = "Validacao ou regra de produto invalida",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Product> create(@AuthenticationPrincipal AppPrincipal principal,
                                          @Valid @RequestBody ProductCreateRequest request) {
        Product created = service.create(CurrentUser.of(principal), request);
        return ResponseEntity.created(URI.create("/products/" + created.id())).body(created);
    }

    @GetMapping("/products/{id}")
    @ApiResponse(responseCode = "200", description = "Produto")
    @ApiResponse(responseCode = "404", description = "Produto nao encontrado",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    public Product get(@PathVariable @Positive long id) {
        return service.get(id);
    }

    @PutMapping("/products/{id}")
    public Product update(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                          @Valid @RequestBody ProductUpdateRequest request) {
        return service.update(CurrentUser.of(principal), id, request);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id) {
        service.delete(CurrentUser.of(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/products/{id}/price")
    public Product changePrice(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                               @Valid @RequestBody PriceChangeRequest request) {
        return service.changePrice(CurrentUser.of(principal), id, request);
    }

    @PatchMapping("/products/{id}/status")
    public Product changeStatus(@AuthenticationPrincipal AppPrincipal principal, @PathVariable @Positive long id,
                                @Valid @RequestBody ProductStatusRequest request) {
        return service.changeStatus(CurrentUser.of(principal), id, request);
    }

    @GetMapping("/products/{id}/price-history")
    public PageResponse<PriceChange> priceHistory(@PathVariable @Positive long id,
                                                  @RequestParam(defaultValue = "0") @Min(0) int page,
                                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.priceHistory(id, page, size);
    }

    @GetMapping("/categories")
    public List<Category> categories() {
        return service.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Category> createCategory(@Valid @RequestBody CategoryRequest request) {
        Category created = service.createCategory(request);
        return ResponseEntity.created(URI.create("/categories/" + created.id())).body(created);
    }

    @PutMapping("/categories/{id}")
    public Category renameCategory(@PathVariable @Positive long id, @Valid @RequestBody CategoryRequest request) {
        return service.renameCategory(id, request);
    }
}
