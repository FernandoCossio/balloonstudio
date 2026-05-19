package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "imagen_articulo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class ImagenArticulo extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticuloInventario articuloInventario;

    @Column(nullable = false)
    private String url;

    @Column(name = "es_principal")
    private Boolean esPrincipal = false;

    @Column
    private Integer orden;

    @Column(name = "procesado_ia")
    private Boolean procesadoIa = false;

    @Column(name = "fecha_subida")
    private LocalDateTime fechaSubida;
}
