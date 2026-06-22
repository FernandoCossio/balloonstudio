package com.decoraciones.features.reserva;

import com.decoraciones.features.inventario.BloqueoInventarioRepository;
import com.decoraciones.features.inventario.ArticuloInventarioRepository;
import com.decoraciones.features.reserva.dto.BloqueoTemporalRedis;
import com.decoraciones.domain.models.ArticuloInventario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventarioLockService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final BloqueoInventarioRepository bloqueoRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final com.decoraciones.features.inventario.IncidenciaArticuloRepository incidenciaRepository;

    @org.springframework.beans.factory.annotation.Value("${app.reserva.lock-ttl-minutes:15}")
    private int lockTtlMinutes;

    private static final String REDIS_PREFIX = "bloqueo:articulo:";

    /**
     * Calcula la cantidad disponible real restando stockTotal - bloqueosPostgres - bloqueosRedis - mermas/incidencias.
     */
    public int getStockDisponible(Long articuloId, LocalDate fechaInicio, LocalDate fechaFin) {
        return getStockDisponible(articuloId, fechaInicio, fechaFin, null);
    }

    public int getStockDisponible(Long articuloId, LocalDate fechaInicio, LocalDate fechaFin, Long proyectoIdExcluir) {
        ArticuloInventario articulo = articuloRepository.findById(articuloId)
                .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado"));

        int totalStock = articulo.getStockTotal() != null ? articulo.getStockTotal() : 0;

        // 1. Bloqueos consolidados en PostgreSQL
        int cantidadBloqueadaPostgres = bloqueoRepository.sumCantidadBloqueadaEnFechas(articuloId, fechaInicio, fechaFin);

        // 2. Bloqueos temporales en Redis (excluyendo el proyecto indicado)
        int cantidadBloqueadaRedis = sumBloqueosTemporalesRedis(articuloId, fechaInicio, fechaFin, proyectoIdExcluir);

        // 3. Mermas e incidencias de mantenimiento activas en PostgreSQL
        int cantidadAfectadaIncidencias = incidenciaRepository.sumCantidadIncidenciasAfectandoFechas(articuloId, fechaInicio, fechaFin);

        int disponible = totalStock - cantidadBloqueadaPostgres - cantidadBloqueadaRedis - cantidadAfectadaIncidencias;
        return Math.max(0, disponible);
    }

    /**
     * Adquiere un bloqueo temporal en Redis por el TTL configurado.
     */
    public boolean lockTemporalmente(Long articuloId, Integer cantidad, LocalDate fechaInicio, LocalDate fechaFin, Long proyectoId) {
        return lockTemporalmente(articuloId, cantidad, fechaInicio, fechaFin, proyectoId, false);
    }

    public boolean lockTemporalmente(Long articuloId, Integer cantidad, LocalDate fechaInicio, LocalDate fechaFin, Long proyectoId, boolean strict) {
        int disponible = getStockDisponible(articuloId, fechaInicio, fechaFin, proyectoId);
        if (disponible < cantidad) {
            log.warn("Stock insuficiente para el articulo ID: {}. Solicitado: {}, Disponible: {}", articuloId, cantidad, disponible);
            return false;
        }

        try {
            String key = REDIS_PREFIX + articuloId + ":proyecto:" + proyectoId + ":random:" + java.util.UUID.randomUUID();
            BloqueoTemporalRedis bloqueo = new BloqueoTemporalRedis(articuloId, cantidad, fechaInicio, fechaFin, proyectoId);

            redisTemplate.opsForValue().set(key, bloqueo, Duration.ofMinutes(lockTtlMinutes));
            log.info("Bloqueo temporal en Redis adquirido con llave: {} por {} unidades.", key, cantidad);
            return true;
        } catch (Exception e) {
            log.error("No se pudo conectar a Redis para guardar bloqueo temporal de articulo ID: {}. Error: {}", articuloId, e.getMessage());
            if (strict) {
                throw new IllegalStateException("Servicio de bloqueo temporal no disponible (Redis Offline): " + e.getMessage(), e);
            }
            // Si Redis falla, en desarrollo o ante desconexión (si no es estricto), permitimos continuar
            return true;
        }
    }

    /**
     * Libera (elimina) todos los bloqueos temporales en Redis asociados a un proyecto.
     */
    public void liberarBloqueosTemporales(Long proyectoId) {
        liberarBloqueosTemporales(proyectoId, false);
    }

    public void liberarBloqueosTemporales(Long proyectoId, boolean strict) {
        try {
            Set<String> keys = redisTemplate.keys(REDIS_PREFIX + "*:proyecto:" + proyectoId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Liberados {} bloqueos temporales de Redis para el proyecto ID: {}", keys.size(), proyectoId);
            }
        } catch (Exception e) {
            log.warn("No se pudo conectar a Redis para liberar bloqueos temporales del proyecto ID: {}. Error: {}", proyectoId, e.getMessage());
            if (strict) {
                throw new IllegalStateException("Servicio de bloqueo temporal no disponible (Redis Offline): " + e.getMessage(), e);
            }
        }
    }

    private int sumBloqueosTemporalesRedis(Long articuloId, LocalDate fechaInicio, LocalDate fechaFin, Long proyectoIdExcluir) {
        try {
            Set<String> keys = redisTemplate.keys(REDIS_PREFIX + articuloId + ":proyecto:*");
            if (keys == null || keys.isEmpty()) {
                return 0;
            }

            int total = 0;
            for (String key : keys) {
                BloqueoTemporalRedis b = (BloqueoTemporalRedis) redisTemplate.opsForValue().get(key);
                if (b != null) {
                    // Excluir bloqueos pertenecientes al mismo proyecto que estamos evaluando
                    if (proyectoIdExcluir != null && proyectoIdExcluir.equals(b.proyectoId())) {
                        continue;
                    }
                    // Verificar solapamiento de fechas
                    if (!(b.fechaInicio().isAfter(fechaFin) || b.fechaFin().isBefore(fechaInicio))) {
                        total += b.cantidad();
                    }
                }
            }
            return total;
        } catch (Exception e) {
            log.warn("No se pudo conectar a Redis para obtener bloqueos temporales. Error: {}", e.getMessage());
            return 0;
        }
    }
}
