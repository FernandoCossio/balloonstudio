package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cotizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Cotizacion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer version = 1;

    @Column(nullable = false)
    private String estado = "PENDIENTE"; // PENDIENTE, ACEPTADA, RECHAZADA, VENCIDA

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "costo_armado", precision = 12, scale = 2)
    private BigDecimal costoArmado;

    @Column(name = "costo_articulos", precision = 12, scale = 2)
    private BigDecimal costoArticulos;

    @Column(name = "costo_flete", precision = 12, scale = 2)
    private BigDecimal costoFlete;

    @Column(name = "tasa_overhead_aplicada", precision = 5, scale = 2)
    private BigDecimal tasaOverheadAplicada;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private ProyectoDiseno proyectoDiseno;
}
