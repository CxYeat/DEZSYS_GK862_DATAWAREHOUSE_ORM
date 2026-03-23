package org.example.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductPurchaseRepository extends JpaRepository<ProductPurchaseEntity, Long> {
    List<ProductPurchaseEntity> findByWarehouseId(Long warehouseId);
    List<ProductPurchaseEntity> findByProductId(UUID productId);
}
