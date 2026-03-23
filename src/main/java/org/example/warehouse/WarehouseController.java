package org.example.warehouse;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path="api/v1/warehouse")
public class WarehouseController {

    private final WarehouseRepository warehouseRepository;

    public WarehouseController(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WarehouseEntity> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WarehouseEntity> getWarehouseById(@PathVariable Long id) {
        return warehouseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/{warehouseId}/product/{productId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductEntity> getProductByWarehouseIdAndProductId(
            @PathVariable Long warehouseId,
            @PathVariable UUID productId) {
        return warehouseRepository.findProductByWarehouseIdAndProductId(warehouseId, productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public WarehouseEntity createWarehouse(@Valid @RequestBody WarehouseEntity entity) {
        return warehouseRepository.save(entity);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WarehouseEntity> updateWarehouse(@PathVariable Long id, @Valid @RequestBody WarehouseEntity entity) {
        return warehouseRepository.findById(id)
                .map(existingWarehouse -> {
                    existingWarehouse.setName(entity.getName());
                    existingWarehouse.setAddress(entity.getAddress());
                    existingWarehouse.setCity(entity.getCity());
                    existingWarehouse.setCountry(entity.getCountry());
                    existingWarehouse.setPostalCode(entity.getPostalCode());
                    existingWarehouse.setProducts(entity.getProducts());
                    return ResponseEntity.ok(warehouseRepository.save(existingWarehouse));
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
