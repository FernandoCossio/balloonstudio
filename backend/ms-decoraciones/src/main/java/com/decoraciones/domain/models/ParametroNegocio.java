package com.decoraciones.domain.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "parametro_negocio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class ParametroNegocio extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Feature Toggles
    @Column(name = "calcular_factor_estacional", nullable = false)
    private Boolean calcularFactorEstacional = true;

    @Column(name = "provision_siniestro_reutilizables", nullable = false)
    private Boolean provisionSiniestroReutilizables = true;

    // Valores y Matrices de Costos
    @Column(name = "costo_overhead_fijo", precision = 12, scale = 2, nullable = false)
    private BigDecimal costoOverheadFijo;

    @Column(name = "capacidad_volumetrica_vehiculo", precision = 8, scale = 3, nullable = false)
    private BigDecimal capacidadVolumetricaVehiculo;

    @Column(name = "tarifa_base_viaje", precision = 12, scale = 2, nullable = false)
    private BigDecimal tarifaBaseViaje;

    @Column(name = "tarifa_km_logistica", precision = 12, scale = 2, nullable = false)
    private BigDecimal tarifaKmLogistica;

    @Column(name = "tarifa_hora_complejidad_baja", precision = 12, scale = 2, nullable = false)
    private BigDecimal tarifaHoraComplejidadBaja;

    @Column(name = "tarifa_hora_complejidad_media", precision = 12, scale = 2, nullable = false)
    private BigDecimal tarifaHoraComplejidadMedia;

    @Column(name = "tarifa_hora_complejidad_alta", precision = 12, scale = 2, nullable = false)
    private BigDecimal tarifaHoraComplejidadAlta;

    @Column(name = "porcentaje_siniestralidad", precision = 5, scale = 2, nullable = false)
    private BigDecimal porcentajeSiniestralidad;

    // Fallbacks
    @Column(name = "fallback_porcentaje_ganancia", precision = 5, scale = 2, nullable = false)
    private BigDecimal fallbackPorcentajeGanancia;

    @Column(name = "fallback_vida_util_usos", nullable = false)
    private Integer fallbackVidaUtilUsos;

    @Column(name = "fallback_vida_util_anos", nullable = false)
    private Integer fallbackVidaUtilAnos;

    @Column(name = "fallback_valor_residual_porcentaje", precision = 5, scale = 2, nullable = false)
    private BigDecimal fallbackValorResidualPorcentaje;

    @Column(name = "fallback_mantenimiento_porcentaje", precision = 5, scale = 2, nullable = false)
    private BigDecimal fallbackMantenimientoPorcentaje;

    @Column(name = "fallback_dias_preparacion", nullable = false)
    private Integer fallbackDiasPreparacion;

    @Column(name = "fallback_dias_limpieza", nullable = false)
    private Integer fallbackDiasLimpieza;
}
