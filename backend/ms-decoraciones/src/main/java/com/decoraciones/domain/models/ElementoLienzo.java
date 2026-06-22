package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "elemento_lienzo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class ElementoLienzo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK denormalizada al proyecto — facilita queries de cotización futura
    // sin necesidad de JOIN extra a escenario_base
    @Column(name = "proyecto_id", nullable = false)
    private Long proyectoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "escenario_id", nullable = false)
    private EscenarioBase escenarioBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticuloInventario articuloInventario;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "pos_x", nullable = false)
    private Double posX;

    @Column(name = "pos_y", nullable = false)
    private Double posY;

    @Column(name = "width")
    private Double width;

    @Column(name = "height")
    private Double height;

    @Column(name = "scale_x")
    private Double scaleX = 1.0;

    @Column(name = "scale_y")
    private Double scaleY = 1.0;

    @Column(name = "rotacion_deg")
    private Double rotacionDeg = 0.0;

    @Column(name = "opacity")
    private Double opacity = 1.0;

    @Column(name = "z_index")
    private Integer zIndex = 0;

    // 'mid' o 'main' — determina en qué ko-layer vive en el canvas
    @Column(name = "layer")
    private String layer = "main";

    @Column(name = "vista_actual")
    private String vistaActual = "FRONTAL";
}
