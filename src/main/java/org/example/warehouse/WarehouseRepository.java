package org.example.warehouse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Long> {
    @Query("SELECT p FROM WarehouseEntity w JOIN w.products p WHERE w.id = :warehouseId AND p.id = :productId")
    Optional<ProductEntity> findProductByWarehouseIdAndProductId(@Param("warehouseId") Long warehouseId, @Param("productId") UUID productId);
}
