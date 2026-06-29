package com.decoraciones.features.reserva;

import com.decoraciones.common.errors.ProyectoDisenoNoEncontradoException;
import com.decoraciones.domain.dtos.cotizacion.CotizacionArticuloDetalle;
import com.decoraciones.domain.dtos.cotizacion.CotizacionDetalleResponse;
import com.decoraciones.domain.dtos.proyectodiseno.ElementoLienzoRequest;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.FactorEstacional;
import com.decoraciones.domain.models.ParametroNegocio;
import com.decoraciones.domain.models.ProyectoDiseno;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import com.decoraciones.features.parametro.ParametroNegocioService;
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
    private final ParametroNegocioService parametroNegocioService;

    /**
     * Calcula la cotización completa y detallada según los elementos en el lienzo.
     */
    @Transactional(readOnly = true)
    public CotizacionDetalleResponse calcularCotizacion(Long proyectoId, List<ElementoLienzoRequest> elementosRequest, Double customDistanciaKm) {
        ProyectoDiseno proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(ProyectoDisenoNoEncontradoException::new);

        ParametroNegocio params = parametroNegocioService.obtenerParametrosActivos();

        LocalDate fechaEvento = proyecto.getFechaEvento() != null ? proyecto.getFechaEvento() : LocalDate.now().plusDays(30);
        
        double distancia = 10.0;
        if (customDistanciaKm != null) {
            distancia = customDistanciaKm;
        } else if (proyecto.getDistanciaKm() != null) {
            distancia = proyecto.getDistanciaKm();
        }

        log.info("=== INICIANDO CÁLCULO DE COTIZACIÓN ===");
        log.info("Proyecto ID: {}, Distancia: {} km, Fecha Evento: {}", proyectoId, distancia, fechaEvento);

        BigDecimal costoArticulos = BigDecimal.ZERO;
        BigDecimal costoArmado = BigDecimal.ZERO;
        double volumenTotal = 0.0;
        int cantidadTotalArticulos = 0;
        List<CotizacionArticuloDetalle> desglose = new ArrayList<>();

        for (ElementoLienzoRequest req : elementosRequest) {
            if (req.cantidad() == null || req.cantidad() <= 0) {
                continue;
            }

            ArticuloInventario art = articuloRepository.findById(req.articuloId())
                    .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado con ID: " + req.articuloId()));

            BigDecimal precioUnitario = calcularPrecioUnitario(art, params);
            BigDecimal precioTotal = precioUnitario.multiply(BigDecimal.valueOf(req.cantidad()));
            costoArticulos = costoArticulos.add(precioTotal);

            // Volumen total
            double volUnitario = art.getVolumenM3() != null ? art.getVolumenM3().doubleValue() : 0.0;
            volumenTotal += volUnitario * req.cantidad();

            // CMO acumulativa por tiempo de armado e ítem
            int tiempoArmado = art.getTiempoArmadoMin() != null ? art.getTiempoArmadoMin() : 0;
            String complexity = art.getNivelComplejidad() != null ? art.getNivelComplejidad().toUpperCase() : "BAJO";
            BigDecimal tarifaComplejidad = params.getTarifaHoraComplejidadBaja();
            if (complexity.equals("ALTO")) {
                tarifaComplejidad = params.getTarifaHoraComplejidadAlta();
            } else if (complexity.equals("MEDIO")) {
                tarifaComplejidad = params.getTarifaHoraComplejidadMedia();
            }

            BigDecimal cmoItem = BigDecimal.valueOf(tiempoArmado)
                    .multiply(BigDecimal.valueOf(req.cantidad()))
                    .multiply(divideSafe(tarifaComplejidad, BigDecimal.valueOf(60)));
            costoArmado = costoArmado.add(cmoItem);

            log.info("[Fórmula CMO] Artículo: '{}' (ID: {}). Cantidad: {}, Tiempo de Armado: {} min, Complejidad: {}, Tarifa/Hora: {} Bs. CMO Calculado: {} Bs.",
                    art.getNombre(), art.getId(), req.cantidad(), tiempoArmado, complexity, tarifaComplejidad, cmoItem);

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

        // Calcular flete (CLT) con escalabilidad por número de viajes
        int numeroViajes = 0;
        BigDecimal costoFlete = BigDecimal.ZERO;
        double capacidad = params.getCapacidadVolumetricaVehiculo() != null ? params.getCapacidadVolumetricaVehiculo().doubleValue() : 8.0;
        if (capacidad <= 0) {
            capacidad = 8.0;
        }

        if (volumenTotal > 0) {
            numeroViajes = (int) Math.ceil(volumenTotal / capacidad);
            if (numeroViajes < 1) {
                numeroViajes = 1;
            }
            BigDecimal tarifaBase = params.getTarifaBaseViaje() != null ? params.getTarifaBaseViaje() : BigDecimal.ZERO;
            BigDecimal tarifaKm = params.getTarifaKmLogistica() != null ? params.getTarifaKmLogistica() : BigDecimal.ZERO;
            costoFlete = tarifaBase.multiply(BigDecimal.valueOf(numeroViajes))
                    .add(BigDecimal.valueOf(distancia).multiply(tarifaKm));

            log.info("[Fórmula CLT] Volumen Total: {} m3, Capacidad Vehículo: {} m3, Viajes requeridos: {}. Tarifa Base/Viaje: {} Bs, Tarifa/Km: {} Bs, Distancia: {} Km. Flete Total: {} Bs.",
                    volumenTotal, capacidad, numeroViajes, tarifaBase, tarifaKm, distancia, costoFlete);
        } else {
            log.info("[Fórmula CLT] Volumen total es 0. Flete establecido en 0 Bs.");
        }

        // Costo fijo por gastos indirectos (Overhead)
        BigDecimal costoOverhead = params.getCostoOverheadFijo() != null ? params.getCostoOverheadFijo() : BigDecimal.ZERO;
        log.info("[Fórmula Overhead] Overhead Fijo aplicado: {} Bs.", costoOverhead);

        // Subtotal de flete + armado + artículos
        BigDecimal subtotal = costoArticulos.add(costoFlete).add(costoArmado);

        // Subtotal con Overhead Fijo inyectado
        BigDecimal subtotalConOverhead = subtotal.add(costoOverhead);

        // Factor Estacional
        BigDecimal factorEstacional = BigDecimal.ONE;
        int mes = fechaEvento.getMonthValue();
        if (Boolean.TRUE.equals(params.getCalcularFactorEstacional())) {
            factorEstacional = factorEstacionalRepository.findByMes(mes)
                    .map(FactorEstacional::getFactorEstacional)
                    .orElse(BigDecimal.ONE);
            log.info("[Fórmula Factor Estacional] Feature Toggle ACTIVO. Mes del evento: {}, Factor Estacional aplicado: {}x", mes, factorEstacional);
        } else {
            log.info("[Fórmula Factor Estacional] Feature Toggle INACTIVO. Factor Estacional forzado a: 1.00x");
        }

        BigDecimal total = subtotalConOverhead.multiply(factorEstacional);
        log.info("[Fórmula PT] PT = (Artículos: {} + Flete/CLT: {} + Armado/CMO: {} + Overhead: {}) * FactorEstacional: {} = Presupuesto Total (PT) Final: {} Bs.",
                costoArticulos, costoFlete, costoArmado, costoOverhead, factorEstacional, total);
        log.info("=== FIN CÁLCULO DE COTIZACIÓN ===");

        return new CotizacionDetalleResponse(
                costoArticulos.setScale(2, RoundingMode.HALF_UP),
                costoFlete.setScale(2, RoundingMode.HALF_UP),
                costoArmado.setScale(2, RoundingMode.HALF_UP),
                costoOverhead.setScale(2, RoundingMode.HALF_UP),
                factorEstacional.setScale(2, RoundingMode.HALF_UP),
                subtotal.setScale(2, RoundingMode.HALF_UP),
                subtotalConOverhead.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP),
                cantidadTotalArticulos,
                BigDecimal.valueOf(volumenTotal).setScale(3, RoundingMode.HALF_UP),
                numeroViajes,
                desglose
        );
    }

    /**
     * Calcula el precio unitario del artículo basándose en su tipo (reutilizable vs consumible) y parámetros dinámicos.
     */
    public BigDecimal calcularPrecioUnitario(ArticuloInventario art, ParametroNegocio params) {
        BigDecimal costoAdq = art.getCostoAdquisicion() != null ? art.getCostoAdquisicion() : BigDecimal.ZERO;

        if (art.getTipoArticulo() != null && art.getTipoArticulo().equalsIgnoreCase("reutilizable")) {
            Integer vidaUsos = art.getVidaUtilUsos();
            Integer vidaAnos = art.getVidaUtilAnos();
            BigDecimal valRes = art.getValorResidual();
            BigDecimal mantenimiento = art.getMantenimientoPromedioBs();
            Integer diasPrep = art.getDiasPreparacionPrevios();
            Integer diasLimp = art.getDiasLimpiezaPosteriores();

            boolean fallbackApplied = false;
            // Si usos y años son nulos/vacíos, inyectamos fallbacks
            if ((vidaUsos == null || vidaUsos <= 0) && (vidaAnos == null || vidaAnos <= 0)) {
                fallbackApplied = true;
                vidaUsos = params.getFallbackVidaUtilUsos();
                vidaAnos = params.getFallbackVidaUtilAnos();
                if (valRes == null) {
                    valRes = costoAdq.multiply(params.getFallbackValorResidualPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                }
                if (mantenimiento == null) {
                    mantenimiento = costoAdq.multiply(params.getFallbackMantenimientoPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                }
                if (diasPrep == null) diasPrep = params.getFallbackDiasPreparacion();
                if (diasLimp == null) diasLimp = params.getFallbackDiasLimpieza();
            } else {
                if (vidaUsos == null || vidaUsos <= 0) vidaUsos = 0;
                if (vidaAnos == null || vidaAnos <= 0) vidaAnos = 0;
                if (valRes == null) {
                    valRes = costoAdq.multiply(params.getFallbackValorResidualPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                }
                if (mantenimiento == null) {
                    mantenimiento = costoAdq.multiply(params.getFallbackMantenimientoPorcentaje().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                }
                if (diasPrep == null) diasPrep = params.getFallbackDiasPreparacion();
                if (diasLimp == null) diasLimp = params.getFallbackDiasLimpieza();
            }

            // Fórmulas
            // PAU = (Costo_Adquisicion / Vida_Util_Usos) + Mantenimiento
            BigDecimal pau = BigDecimal.ZERO;
            if (vidaUsos > 0) {
                pau = divideSafe(costoAdq, BigDecimal.valueOf(vidaUsos)).add(mantenimiento);
            }

            // DLA = ((Costo_Adquisicion - Valor_Residual) / (Vida_Util_Anios * 365)) * (1 + Dias_Preparacion + Dias_Limpieza)
            BigDecimal dla = BigDecimal.ZERO;
            if (vidaAnos > 0) {
                BigDecimal amortizacion = costoAdq.subtract(valRes);
                BigDecimal diasTotalVida = BigDecimal.valueOf(vidaAnos * 365L);
                BigDecimal costoPorDia = divideSafe(amortizacion, diasTotalVida);
                BigDecimal diasAfectados = BigDecimal.valueOf(1 + diasPrep + diasLimp);
                dla = costoPorDia.multiply(diasAfectados);
            }

            // Provisión por Siniestro (PS)
            BigDecimal ps = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(params.getProvisionSiniestroReutilizables())) {
                ps = costoAdq.multiply(params.getPorcentajeSiniestralidad().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            }

            // PAR unitario final
            BigDecimal ganancia = art.getPorcentajeGanancia() != null ? art.getPorcentajeGanancia() : params.getFallbackPorcentajeGanancia();
            if (ganancia == null) {
                ganancia = BigDecimal.ZERO;
            }
            BigDecimal factorGanancia = BigDecimal.ONE.add(ganancia.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal mayorBase = pau.max(dla);
            BigDecimal parUnitario = mayorBase.add(ps).multiply(factorGanancia);

            log.info("[Fórmula PAR Reutilizable] Artículo: '{}' (ID: {}). Costo Adq: {} Bs, Vida Usos: {}, Vida Años: {}, Val Residual: {} Bs, Mantenimiento: {} Bs, Días Prep: {}, Días Limpieza: {}. PAU: {} Bs, DLA: {} Bs, PS: {} Bs, Ganancia: {}%. Fallbacks Aplicados: {}. PAR Unitario Resultante: {} Bs.",
                    art.getNombre(), art.getId(), costoAdq, vidaUsos, vidaAnos, valRes, mantenimiento, diasPrep, diasLimp, pau, dla, ps, ganancia, fallbackApplied, parUnitario);

            return parUnitario;
        } else {
            // Consumible: PC = Costo_Adquisicion * (1 + (Porcentaje_Markup / 100))
            BigDecimal markup = art.getPorcentajeGanancia() != null ? art.getPorcentajeGanancia() : params.getFallbackPorcentajeGanancia();
            if (markup == null) {
                markup = BigDecimal.ZERO;
            }
            BigDecimal factorGanancia = BigDecimal.ONE.add(markup.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            BigDecimal pcUnitario = costoAdq.multiply(factorGanancia);

            log.info("[Fórmula PC Consumible] Artículo: '{}' (ID: {}). Costo Adq: {} Bs, Markup: {}%. PC Unitario Resultante: {} Bs.",
                    art.getNombre(), art.getId(), costoAdq, markup, pcUnitario);

            return pcUnitario;
        }
    }

    /**
     * Fallback deprecated signature for compatibility or external tests.
     */
    public BigDecimal calcularPrecioUnitario(ArticuloInventario art) {
        ParametroNegocio params = parametroNegocioService.obtenerParametrosActivos();
        return calcularPrecioUnitario(art, params);
    }

    private BigDecimal divideSafe(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
