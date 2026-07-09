package com.decoraciones.seed;

import com.decoraciones.domain.models.ParametroNegocio;
import com.decoraciones.features.parametro.ParametroNegocioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
public class ParametroNegocioSeeder implements CommandLineRunner {

    private final ParametroNegocioRepository repository;

    public ParametroNegocioSeeder(ParametroNegocioRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        seedParametros();
    }

    private void seedParametros() {
        if (repository.count() == 0) {
            ParametroNegocio param = new ParametroNegocio();
            param.setCalcularFactorEstacional(true);
            param.setProvisionSiniestroReutilizables(true);
            param.setCostoOverheadFijo(BigDecimal.valueOf(250.00));
            param.setCapacidadVolumetricaVehiculo(BigDecimal.valueOf(8.0));
            param.setTarifaBaseViaje(BigDecimal.valueOf(150.00));
            param.setTarifaKmLogistica(BigDecimal.valueOf(5.00));
            param.setTarifaHoraComplejidadBaja(BigDecimal.valueOf(35.00));
            param.setTarifaHoraComplejidadMedia(BigDecimal.valueOf(60.00));
            param.setTarifaHoraComplejidadAlta(BigDecimal.valueOf(100.00));
            param.setPorcentajeSiniestralidad(BigDecimal.valueOf(2.00));
            
            param.setFallbackPorcentajeGanancia(BigDecimal.valueOf(20.00));
            param.setFallbackVidaUtilUsos(50);
            param.setFallbackVidaUtilAnos(3);
            param.setFallbackValorResidualPorcentaje(BigDecimal.valueOf(10.00));
            param.setFallbackMantenimientoPorcentaje(BigDecimal.valueOf(2.00));
            param.setFallbackDiasPreparacion(1);
            param.setFallbackDiasLimpieza(1);

            repository.save(param);
            System.out.println("Parámetros de negocio inicializados en la base de datos.");
        }
    }
}
