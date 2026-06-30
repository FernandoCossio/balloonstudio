package com.decoraciones.features.configuracion;

import com.decoraciones.common.errors.AppException;
import com.decoraciones.common.errors.ErrorCode;
import com.decoraciones.domain.models.Configuracion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfiguracionService {

    private final ConfiguracionRepository configuracionRepository;

    @Transactional(readOnly = true)
    public List<Configuracion> findAll() {
        return configuracionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Configuracion findByClave(String clave) {
        return configuracionRepository.findByClave(clave)
                .orElseGet(() -> {
                    Configuracion conf = new Configuracion();
                    conf.setClave(clave);
                    conf.setValor(getDefaultValue(clave));
                    conf.setDescripcion("Valor por defecto auto-generado");
                    return conf;
                });
    }

    public Configuracion create(Configuracion configuracion) {
        if (configuracionRepository.findByClave(configuracion.getClave()).isPresent()) {
            throw new AppException(ErrorCode.ERROR_INTERNO, "La clave de configuración ya existe: " + configuracion.getClave());
        }
        return configuracionRepository.save(configuracion);
    }

    public Configuracion update(String clave, String valor, String descripcion) {
        Configuracion configuracion = configuracionRepository.findByClave(clave)
                .orElseGet(() -> {
                    Configuracion conf = new Configuracion();
                    conf.setClave(clave);
                    return conf;
                });
        configuracion.setValor(valor);
        if (descripcion != null) {
            configuracion.setDescripcion(descripcion);
        }
        return configuracionRepository.save(configuracion);
    }

    public void delete(Long id) {
        if (!configuracionRepository.existsById(id)) {
            throw new AppException(ErrorCode.ERROR_INTERNO, "Configuración no encontrada con ID: " + id);
        }
        configuracionRepository.deleteById(id);
    }

    private String getDefaultValue(String clave) {
        return switch (clave) {
            case "EMPRESA_NOMBRE" -> "Balloon Studio";
            case "EMPRESA_NIT" -> "1029384756";
            case "EMPRESA_DIRECCION" -> "Av. Banzer 4to Anillo, Santa Cruz de la Sierra, Bolivia";
            case "EMPRESA_TELEFONO" -> "+591 75540850";
            case "EMPRESA_EMAIL" -> "info@balloonstudio.com";
            case "EMPRESA_LATITUD" -> "-17.7818";
            case "EMPRESA_LONGITUD" -> "-63.1804";
            case "RECIBO_PI_PAGINA" -> "Gracias por su confianza. Los anticipos del 20% no son reembolsables en caso de cancelación.";
            default -> "";
        };
    }
}
