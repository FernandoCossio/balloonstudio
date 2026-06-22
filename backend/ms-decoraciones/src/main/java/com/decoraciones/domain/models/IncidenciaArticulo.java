package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia_articulo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class IncidenciaArticulo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticuloInventario articuloInventario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Column(nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String tipo; // REPARACION, MERMA_PERDIDA

    @Column(nullable = false)
    private String estado = "ACTIVA"; // ACTIVA, SOLUCIONADA

    @Column(name = "cantidad_afectada", nullable = false)
    private Integer cantidadAfectada = 1;

    @Column(name = "fecha_incidencia", nullable = false)
    private LocalDate fechaIncidencia;

    @Column(name = "fecha_resolucion_estimada")
    private LocalDate fechaResolucionEstimada;

    @Column(name = "costo_reparacion", precision = 12, scale = 2)
    private java.math.BigDecimal costoReparacion;
}
