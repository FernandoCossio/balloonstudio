package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;

@Entity
@Table(name = "factor_estacional")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class FactorEstacional extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer mes; // 1 = Enero, 12 = Diciembre

    @Column
    private String descripcion;

    @Column(name = "factor_estacional", nullable = false, precision = 5, scale = 2)
    private BigDecimal factorEstacional;
}
