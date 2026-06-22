package com.decoraciones.features.inventario;

import com.decoraciones.common.errors.ArticuloInventarioNoEncontradoException;
import com.decoraciones.common.errors.IncidenciaNoEncontradaException;
import com.decoraciones.common.errors.ReservaNoEncontradaException;
import com.decoraciones.domain.dtos.incidencia.IncidenciaRequest;
import com.decoraciones.domain.dtos.incidencia.SolucionarIncidenciaRequest;
import com.decoraciones.domain.models.ArticuloInventario;
import com.decoraciones.domain.models.IncidenciaArticulo;
import com.decoraciones.domain.models.Reserva;
import com.decoraciones.features.reserva.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidenciaService {

    private final IncidenciaArticuloRepository incidenciaRepository;
    private final ArticuloInventarioRepository articuloRepository;
    private final ReservaRepository reservaRepository;

    /**
     * Reportar una nueva incidencia (REPARACION o MERMA_PERDIDA).
     */
    @Transactional
    public IncidenciaArticulo reportarIncidencia(IncidenciaRequest request) {
        ArticuloInventario articulo = articuloRepository.findById(request.articuloId())
                .orElseThrow(ArticuloInventarioNoEncontradoException::new);

        IncidenciaArticulo incidencia = new IncidenciaArticulo();
        incidencia.setArticuloInventario(articulo);
        incidencia.setDescripcion(request.descripcion());
        incidencia.setTipo(request.tipo().toUpperCase());
        incidencia.setEstado("ACTIVA");
        incidencia.setCantidadAfectada(request.cantidad());
        incidencia.setFechaIncidencia(LocalDate.now());

        if (request.reservaId() != null) {
            Reserva reserva = reservaRepository.findById(request.reservaId())
                    .orElseThrow(ReservaNoEncontradaException::new);
            incidencia.setReserva(reserva);
        }

        if (request.fechaResolucionEstimada() != null && !request.fechaResolucionEstimada().isBlank()) {
            incidencia.setFechaResolucionEstimada(LocalDate.parse(request.fechaResolucionEstimada()));
        }

        return incidenciaRepository.save(incidencia);
    }

    /**
     * Solucionar una incidencia (libera el stock bloqueado por reparación).
     */
    @Transactional
    public IncidenciaArticulo solucionarIncidencia(Long id, SolucionarIncidenciaRequest request) {
        IncidenciaArticulo incidencia = incidenciaRepository.findById(id)
                .orElseThrow(IncidenciaNoEncontradaException::new);

        incidencia.setEstado("SOLUCIONADA");
        incidencia.setFechaResolucionEstimada(LocalDate.now());
        
        if (request != null && request.costoReparacion() != null) {
            incidencia.setCostoReparacion(request.costoReparacion());
        }

        return incidenciaRepository.save(incidencia);
    }

    /**
     * Listar todas las incidencias.
     */
    public List<IncidenciaArticulo> listarIncidencias() {
        return incidenciaRepository.findAll();
    }
}
