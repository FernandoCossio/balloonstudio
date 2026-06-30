package com.decoraciones.features.parametro;

import com.decoraciones.domain.models.ParametroNegocio;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParametroNegocioService {

    private final ParametroNegocioRepository repository;

    @Transactional(readOnly = true)
    @Cacheable(value = "parametros_negocio", key = "'1'")
    public ParametroNegocio obtenerParametrosActivos() {
        return repository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Los parámetros de negocio no han sido inicializados."));
    }

    @CacheEvict(value = "parametros_negocio", key = "'1'")
    public ParametroNegocio actualizarParametros(ParametroNegocio nuevosParametros) {
        ParametroNegocio actual = repository.findAll().stream().findFirst()
                .orElseGet(() -> {
                    return repository.save(nuevosParametros);
                });

        actual.setCalcularFactorEstacional(nuevosParametros.getCalcularFactorEstacional());
        actual.setProvisionSiniestroReutilizables(nuevosParametros.getProvisionSiniestroReutilizables());
        actual.setCostoOverheadFijo(nuevosParametros.getCostoOverheadFijo());
        actual.setCapacidadVolumetricaVehiculo(nuevosParametros.getCapacidadVolumetricaVehiculo());
        actual.setTarifaBaseViaje(nuevosParametros.getTarifaBaseViaje());
        actual.setTarifaKmLogistica(nuevosParametros.getTarifaKmLogistica());
        actual.setTarifaHoraComplejidadBaja(nuevosParametros.getTarifaHoraComplejidadBaja());
        actual.setTarifaHoraComplejidadMedia(nuevosParametros.getTarifaHoraComplejidadMedia());
        actual.setTarifaHoraComplejidadAlta(nuevosParametros.getTarifaHoraComplejidadAlta());
        actual.setPorcentajeSiniestralidad(nuevosParametros.getPorcentajeSiniestralidad());
        
        actual.setFallbackPorcentajeGanancia(nuevosParametros.getFallbackPorcentajeGanancia());
        actual.setFallbackVidaUtilUsos(nuevosParametros.getFallbackVidaUtilUsos());
        actual.setFallbackVidaUtilAnos(nuevosParametros.getFallbackVidaUtilAnos());
        actual.setFallbackValorResidualPorcentaje(nuevosParametros.getFallbackValorResidualPorcentaje());
        actual.setFallbackMantenimientoPorcentaje(nuevosParametros.getFallbackMantenimientoPorcentaje());
        actual.setFallbackDiasPreparacion(nuevosParametros.getFallbackDiasPreparacion());
        actual.setFallbackDiasLimpieza(nuevosParametros.getFallbackDiasLimpieza());

        return repository.save(actual);
    }
}
