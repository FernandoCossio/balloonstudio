package com.decoraciones.domain.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(name = "empleado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Empleado extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Column(nullable = false, unique = true)
    private String ci;

    @Column(nullable = false)
    private String cargo;

    @Column
    private String telefono;

    @Column(unique = true)
    private String email;

    @Column(name = "fecha_contratacion")
    private LocalDate fechaContratacion;

    @Column(nullable = false)
    private Boolean activo = true;
}
