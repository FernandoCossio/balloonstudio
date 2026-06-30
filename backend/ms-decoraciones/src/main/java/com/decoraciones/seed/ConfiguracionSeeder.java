package com.decoraciones.seed;

import com.decoraciones.domain.models.Configuracion;
import com.decoraciones.features.configuracion.ConfiguracionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class ConfiguracionSeeder implements CommandLineRunner {

    private final ConfiguracionRepository configuracionRepository;

    public ConfiguracionSeeder(ConfiguracionRepository configuracionRepository) {
        this.configuracionRepository = configuracionRepository;
    }

    @Override
    public void run(String... args) {
        seedConfiguraciones();
    }

    private void seedConfiguraciones() {
        seedConfig("EMPRESA_NOMBRE", "Balloon Studio", "Nombre comercial de la empresa");
        seedConfig("EMPRESA_NIT", "1029384756", "NIT / Identificación tributaria");
        seedConfig("EMPRESA_DIRECCION", "Av. Banzer 4to Anillo, Santa Cruz de la Sierra, Bolivia", "Dirección física de la oficina central");
        seedConfig("EMPRESA_TELEFONO", "+591 75540850", "Teléfono principal de contacto");
        seedConfig("EMPRESA_EMAIL", "info@balloonstudio.com", "Correo electrónico principal de soporte");
        seedConfig("EMPRESA_LATITUD", "-17.7818", "Latitud de la ubicación de la oficina");
        seedConfig("EMPRESA_LONGITUD", "-63.1804", "Longitud de la ubicación de la oficina");
        seedConfig("RECIBO_PI_PAGINA", "Gracias por su confianza. Los anticipos del 20% no son reembolsables en caso de cancelación.", "Mensaje o nota legal al pie de los recibos");
    }

    private void seedConfig(String clave, String valor, String descripcion) {
        if (configuracionRepository.findByClave(clave).isEmpty()) {
            Configuracion config = new Configuracion();
            config.setClave(clave);
            config.setValor(valor);
            config.setDescripcion(descripcion);
            configuracionRepository.save(config);
            System.out.println("Configuración semilla creada: " + clave);
        }
    }
}
