package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "escenario_base")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class EscenarioBase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column
    private String descripcion;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(name = "imagen_diseno_url")
    private String imagenDisenoUrl;

    @Column(name = "dimensiones_alto_px")
    private Integer dimensionesAltoPx;

    @Column(name = "dimensiones_ancho_px")
    private Integer dimensionesAnchoPx;

    @Column
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_diseno_id", nullable = false)
    private ProyectoDiseno proyectoDiseno;

    @OneToMany(mappedBy = "escenarioBase", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("zIndex ASC")
    private List<ElementoLienzo> elementos = new ArrayList<>();
}
