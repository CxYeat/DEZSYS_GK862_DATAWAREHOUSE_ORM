package org.example.warehouse;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Produktname darf nicht leer sein")
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @NotBlank(message = "Kategorie darf nicht leer sein")
    @Size(max = 100)
    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @NotNull(message = "Menge darf nicht null sein")
    @Min(value = 0, message = "Menge darf nicht negativ sein")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotBlank(message = "Einheit darf nicht leer sein")
    @Size(max = 20)
    @Column(name = "unit", nullable = false, length = 20)
    private String unit;
}
