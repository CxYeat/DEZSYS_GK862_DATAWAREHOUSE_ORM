package org.example.warehouse;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "warehouse")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @NotBlank(message = "Warehouse name darf nicht leer sein")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Adresse darf nicht leer sein")
    @Size(max = 100)
    @Column(name = "address", nullable = false)
    private String address;

    @NotBlank(message = "Postal Code darf nicht leer sein")
    @Size(max = 10)
    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @NotBlank(message = "Postal Code darf nicht leer sein")
    @Size(max = 50)
    @Column(name = "city", nullable = false)
    private String city;

    @NotBlank(message = "Postal Code darf nicht leer sein")
    @Size(max = 50)
    @Column(name = "country", nullable = false)
    private String country;

    @JoinColumn(name = "products")
    @OneToMany
    private List<ProductEntity> products;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
