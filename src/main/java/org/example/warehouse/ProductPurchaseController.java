package org.example.warehouse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/purchase")
public class ProductPurchaseController {

    private final ProductPurchaseRepository purchaseRepository;

    public ProductPurchaseController(ProductPurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductPurchaseEntity> getAllPurchases() {
        return purchaseRepository.findAll();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductPurchaseEntity createPurchase(@Valid @RequestBody ProductPurchaseEntity purchase) {
        return purchaseRepository.save(purchase);
    }

    @GetMapping(value = "/warehouse/{warehouseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProductPurchaseEntity> getPurchasesByWarehouse(@PathVariable Long warehouseId) {
        return purchaseRepository.findByWarehouseId(warehouseId);
    }
}
