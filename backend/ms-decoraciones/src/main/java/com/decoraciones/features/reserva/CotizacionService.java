package com.decoraciones.features.reserva;

import com.decoraciones.common.errors.ProyectoDisenoNoEncontradoException;
import com.decoraciones.domain.dtos.cotizacion.CotizacionArticuloDetalle;
import com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.FactorEstacional;
import com.decoraciones.domain.models.ProyectoDiseno;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import com.decoraciones.features.proyectodiseno.ProyectoDisenoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CotizacionService {

    private final ProyectoDisenoRepository proyectoRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final FactorEstacionalRepository factorEstacionalRepository;

    /**
     * Calcula la cotización completa y detallada según los elementos en el lienzo.
     */
    @Transactional(readOnly = true)
    public CotizacionDetalleResponse calcularCotizacion(Long proyectoId, List<ElementoLienzoRequest> elementosRequest, Double customDistanciaKm) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        LocalDate fechaEvento = proyecto.getFechaEvento() != null ? proyecto.getFechaEvento() : LocalDate.now().plusDays(30);
        
        double distancia = 10.0;
        if (customDistanciaKm != null) {
            distancia = customDistanciaKm;
        } else if (proyecto.getDistanciaKm() != null) {
            distancia = proyecto.getDistanciaKm();
        }

        BigDecimal costoArticulos = BigDecimal.ZERO;
        double volumenTotal = 0.0;
        String maxComplexity = "BAJO";
        int cantidadTotalArticulos = 0;
        List<CotizacionArticuloDetalle> desglose = new ArrayList<>();

        for (ElementoLienzoRequest req : elementosRequest) {
            if (req.cantidad() == null || req.cantidad() <= 0) {
                continue;
            }

            ArticuloInventario art = articuloRepository.findById(req.articuloId())
                    .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado con ID: " + req.articuloId()));

            BigDecimal precioUnitario = calcularPrecioUnitario(art);
            BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(req.cantidad()));
            costoArticulos = costoArticulos.add(precioTotal);

            // Volumen total
            double volUnitario = art.getVolumenM3() != null ? art.getVolumenM3().doubleValue() : 0.0;
            volumenTotal += volUnitario * req.cantidad();

            // Complejidad máxima
            String complexity = art.getNivelComplejidad() != null ? art.getNivelComplejidad().toUpperCase() : "BAJO";
            if (complexity.equals("ALTO")) {
                maxComplexity = "ALTO";
            } else if (complexity.equals("MEDIO") && !maxComplexity.equals("ALTO")) {
                maxComplexity = "MEDIO";
            }

            cantidadTotalArticulos += req.cantidad();

            desglose.add(new CotizacionArticuloDetalle(
                    art.getId(),
                    art.getNombre(),
                    req.cantidad(),
                    precioUnitario.setScale(2, RoundingMode.HALF_UP),
                    precioTotal.setScale(2, RoundingMode.HALF_UP),
                    art.getTipoArticulo(),
                    art.getNivelComplejidad()
            ));
        }

        // Calcular flete (CLT)
        BigDecimal costoFlete = calcularFlete(volumenTotal, distancia);

        // Calcular mano de obra (CMO)
        BigDecimal costoArmado = calcularManoObra(maxComplexity, cantidadTotalArticulos);

        // Subtotal de flete + armado + artículos
        BigDecimal subtotal = costoArticulos.add(costoFlete).add(costoArmado);

        // Overhead (10%)
        BigDecimal tasaOverhead = BigDecimal.valueOf(10.00); // 10%
        BigDecimal overhead = subtotal.multiply(BigDecimal.valueOf(0.10));
        BigDecimal subtotalConOverhead = subtotal.add(overhead);

        // Factor Estacional
        int mes = fechaEvento.getMonthValue();
        BigDecimal factorEstacional = factorEstacionalRepository.findByMes(mes)
                .map(FactorEstacional::getFactorEstacional)
                .orElse(BigDecimal.ONE);

        BigDecimal total = subtotalConOverhead.multiply(factorEstacional);

        return new CotizacionDetalleResponse(
                costoArticulos.setScale(2, RoundingMode.HALF_UP),
                costoFlete.setScale(2, RoundingMode.HALF_UP),
                costoArmado.setScale(2, RoundingMode.HALF_UP),
                tasaOverhead.setScale(2, RoundingMode.HALF_UP),
                factorEstacional.setScale(2, RoundingMode.HALF_UP),
                subtotal.setScale(2, RoundingMode.HALF_UP),
                subtotalConOverhead.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP),
                cantidadTotalArticulos,
                desglose
        );
    }

    /**
     * Calcula el precio unitario del artículo basándose en su tipo (reutilizable vs consumible).
     */
    public BigDecimal calcularPrecioUnitario(ArticuloInventario art) {
        BigDecimal costoAdq = art.getCostoAdquisicion() != null ? art.getCostoAdquisicion() : BigDecimal.ZERO;
        BigDecimal ganancia = art.getPorcentajeGanancia() != null ? art.getPorcentajeGanancia() : BigDecimal.ZERO;
        BigDecimal factorGanancia = BigDecimal.ONE.add(ganancia.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));

        if (art.getTipoArticulo() != null && art.getTipoArticulo().equalsIgnoreCase("reutilizable")) {
            BigDecimal valRes = art.getValorResidual() != null ? art.getValorResidual() : BigDecimal.ZERO;
            BigDecimal mantenimiento = art.getMantenimientoPromedioBs() != null ? art.getMantenimientoPromedioBs() : BigDecimal.ZERO;

            Integer diasPrep = art.getDiasPreparacionPrevios() != null ? art.getDiasPreparacionPrevios() : 0;
            Integer diasLimp = art.getDiasLimpiezaPosteriores() != null ? art.getDiasLimpiezaPosteriores() : 0;
            Integer vidaUsos = art.getVidaUtilUsos() != null ? art.getVidaUtilUsos() : 0;
            Integer vidaAnos = art.getVidaUtilAnos() != null ? art.getVidaUtilAnos() : 0;

            // FALLBACK ROBUSTO: Si no se configuraron usos ni años, aplicar parámetros por defecto razonables
            if (vidaUsos <= 0 && vidaAnos <= 0) {
                log.warn("Artículo reutilizable ID: {} '{}' no tiene configurados vida_util_usos ni vida_util_anos. Aplicando fallback de amortización estándar.", art.getId(), art.getNombre());
                vidaUsos = 50; // 50 usos estimados
                vidaAnos = 3;  // 3 años estimados
                if (art.getValorResidual() == null) {
                    valRes = costoAdq.multiply(BigDecimal.valueOf(0.10)); // 10% valor residual
                }
                if (art.getMantenimientoPromedioBs() == null) {
                    mantenimiento = costoAdq.multiply(BigDecimal.valueOf(0.02)); // 2% mantenimiento
                }
                diasPrep = 1;
                diasLimp = 1;
            }

            // Fórmulas
            // PAU = (costoAdq - valRes) / vidaUsos + mantenimiento
            BigDecimal pau = BigDecimal.ZERO;
            if (vidaUsos > 0) {
                BigDecimal amortizacion = costoAdq.subtract(valRes);
                pau = divideSafe(amortizacion, BigDecimal.valueOf(vidaUsos)).add(mantenimiento);
            }

            // DLA = ((costoAdq - valRes) / (vidaAnos * 365)) * (1 + diasPrep + diasLimp)
            BigDecimal dla = BigDecimal.ZERO;
            if (vidaAnos > 0) {
                BigDecimal amortizacion = costoAdq.subtract(valRes);
                BigDecimal diasTotalVida = BigDecimal.valueOf(vidaAnos * 365L);
                BigDecimal costoPorDia = divideSafe(amortizacion, diasTotalVida);
                BigDecimal diasAfectados = BigDecimal.valueOf(1 + diasPrep + diasLimp);
                dla = costoPorDia.multiply(diasAfectados);
            }

            BigDecimal mayorBase = pau.max(dla);
            return mayorBase.multiply(factorGanancia);
        } else {
            // Consumible: Precio = costoAdq * (1 + ganancia/100)
            return costoAdq.multiply(factorGanancia);
        }
    }

    private BigDecimal calcularFlete(double volumenM3, double distanciaKm) {
        double base = 0.0;
        if (volumenM3 <= 1.5) {
            base = 100.0;
        } else if (volumenM3 <= 8.0) {
            base = 250.0;
        } else {
            base = 500.0;
        }
        double totalFlete = base + (distanciaKm * 5.0);
        return BigDecimal.valueOf(totalFlete);
    }

    private BigDecimal calcularManoObra(String maxComplexity, int cantidadTotal) {
        if (cantidadTotal == 0) {
            return BigDecimal.ZERO;
        }
        double base = 50.0; // BAJO
        if (maxComplexity.equalsIgnoreCase("ALTO")) {
            base = 350.0;
        } else if (maxComplexity.equalsIgnoreCase("MEDIO")) {
            base = 150.0;
        }
        return BigDecimal.valueOf(base);
    }

    private BigDecimal divideSafe(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
