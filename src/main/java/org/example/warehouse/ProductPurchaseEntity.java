package org.example.warehouse;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_purchase")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductPurchaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product darf nicht leer sein")
    private ProductEntity product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    @NotNull(message = "Warehouse darf nicht leer sein")
    private WarehouseEntity warehouse;

    @NotNull(message = "Menge darf nicht null sein")
    @Min(value = 1, message = "Menge muss mindestens 1 sein")
    @Column(name = "amount", nullable = false)
    private Integer amount;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
