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
@Table(name = "pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class Pago extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", nullable = false)
    private Reserva reserva;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal monto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id", nullable = false)
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private String estado = "COMPLETADO"; // COMPLETADO, RECHAZADO, PENDIENTE

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @Column(name = "referencia_externa")
    private String referenciaExterna;

    @Column(name = "tipo_pago")
    private String tipoPago; // ANTICIPO, LIQUIDACION
}
