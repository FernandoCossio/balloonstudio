package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "articulo_inventario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class ArticuloInventario extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String descripcion;

    @Column(name = "tipo_articulo")
    private String tipoArticulo;

    @Column(name = "estado")
    private String estado;

    @Column(name = "costo_adquisicion", precision = 12, scale = 2)
    private BigDecimal costoAdquisicion;

    @Column(name = "porcentaje_ganancia", precision = 5, scale = 2)
    private BigDecimal porcentajeGanancia;

    @Column(name = "valor_residual", precision = 12, scale = 2)
    private BigDecimal valorResidual;

    @Column(name = "vida_util_anos")
    private Integer vidaUtilAnos;

    @Column(name = "vida_util_usos")
    private Integer vidaUtilUsos;

    @Column(name = "stock_total")
    private Integer stockTotal;

    @Column(name = "peso_kg", precision = 8, scale = 3)
    private BigDecimal pesoKg;

    @Column(name = "volumen_m3", precision = 8, scale = 3)
    private BigDecimal volumenM3;

    @Column(name = "tiempo_armado_min")
    private Integer tiempoArmadoMin;

    @Column(name = "dias_preparacion_previos")
    private Integer diasPreparacionPrevios;

    @Column(name = "dias_limpieza_posteriores")
    private Integer diasLimpiezaPosteriores;

    @Column(name = "mantenimiento_promedio_bs", precision = 12, scale = 2)
    private BigDecimal mantenimientoPromedioBs;

    @Column(name = "nivel_complejidad")
    private String nivelComplejidad;

    @Column(name = "embedding_visual", columnDefinition = "vector(512)", insertable = false, updatable = false)
    private String embeddingVisual;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "categoria_inventario",
            joinColumns = @JoinColumn(name = "inventario_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    private Set<Categoria> categorias = new HashSet<>();

    @OneToMany(mappedBy = "articuloInventario", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("orden ASC")
    private List<ImagenArticulo> imagenes = new ArrayList<>();
}
