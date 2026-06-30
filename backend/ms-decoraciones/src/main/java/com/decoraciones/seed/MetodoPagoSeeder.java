package com.decoraciones.seed;

import com.decoraciones.domain.models.MetodoPago;
import com.decoraciones.features.pago.MetodoPagoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class MetodoPagoSeeder implements CommandLineRunner {

    private final MetodoPagoRepository metodoPagoRepository;

    public MetodoPagoSeeder(MetodoPagoRepository metodoPagoRepository) {
        this.metodoPagoRepository = metodoPagoRepository;
    }

    @Override
    public void run(String... args) {
        seedMetodosPago();
    }

    private void seedMetodosPago() {
        seedMetodo("STRIPE", "Plataforma internacional de pagos con tarjeta de crédito/débito");
        seedMetodo("PAGO_FACIL", "Plataforma de pagos local por QR y transferencias");
    }

    private void seedMetodo(String nombre, String descripcion) {
        if (metodoPagoRepository.findByNombre(nombre).isEmpty()) {
            MetodoPago mp = new MetodoPago();
            mp.setNombre(nombre);
            mp.setDescripcion(descripcion);
            metodoPagoRepository.save(mp);
            System.out.println("Método de Pago creado: " + nombre);
        }
    }
}
