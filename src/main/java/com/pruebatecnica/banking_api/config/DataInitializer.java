package com.pruebatecnica.banking_api.config;

import com.pruebatecnica.banking_api.domain.Cliente;
import com.pruebatecnica.banking_api.domain.Cuenta;
import com.pruebatecnica.banking_api.domain.Movimiento;
import com.pruebatecnica.banking_api.repository.ClienteRepository;
import com.pruebatecnica.banking_api.repository.CuentaRepository;
import com.pruebatecnica.banking_api.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Carga datos de prueba al iniciar la aplicación.
 *
 * Clientes precargados:
 *   ID 1 - Ana García        (DNI: 12345678)
 *   ID 2 - Carlos López      (DNI: 87654321)
 *
 * Cuentas precargadas:
 *   ID 1 - 0001-0001 AHORRO    $5,000.00  (Ana)
 *   ID 2 - 0001-0002 CORRIENTE $12,500.00 (Ana)
 *   ID 3 - 0002-0001 AHORRO    $3,200.00  (Carlos)
 *   ID 4 - 0002-0002 CORRIENTE $800.00    (Carlos)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final MovimientoRepository movimientoRepository;

    @Override
    public void run(String... args) {
        // Clientes
        Cliente ana = clienteRepository.save(new Cliente("Ana García", "12345678"));
        Cliente carlos = clienteRepository.save(new Cliente("Carlos López", "87654321"));

        // Cuentas de Ana
        Cuenta anaAhorro = cuentaRepository.save(
                new Cuenta("0001-0001", Cuenta.Tipo.AHORRO, new BigDecimal("5000.00"), ana));
        Cuenta anaCorriente = cuentaRepository.save(
                new Cuenta("0001-0002", Cuenta.Tipo.CORRIENTE, new BigDecimal("12500.00"), ana));

        // Cuentas de Carlos
        Cuenta carlosAhorro = cuentaRepository.save(
                new Cuenta("0002-0001", Cuenta.Tipo.AHORRO, new BigDecimal("3200.00"), carlos));
        Cuenta carlosCorriente = cuentaRepository.save(
                new Cuenta("0002-0002", Cuenta.Tipo.CORRIENTE, new BigDecimal("800.00"), carlos));

        // Movimientos históricos de ejemplo
        saveMovimiento(Movimiento.Tipo.DEPOSITO, "2000.00", "Depósito inicial", anaAhorro);
        saveMovimiento(Movimiento.Tipo.DEPOSITO, "5000.00", "Depósito inicial", anaCorriente);
        saveMovimiento(Movimiento.Tipo.RETIRO,   "500.00",  "Pago servicios",   anaCorriente);
        saveMovimiento(Movimiento.Tipo.DEPOSITO, "1500.00", "Depósito inicial", carlosAhorro);
        saveMovimiento(Movimiento.Tipo.DEPOSITO, "800.00",  "Depósito inicial", carlosCorriente);

        log.info("=== Datos precargados correctamente ===");
        log.info("Cliente Ana García   → ID: {}", ana.getId());
        log.info("  Cuenta ahorro      → ID: {}", anaAhorro.getId());
        log.info("  Cuenta corriente   → ID: {}", anaCorriente.getId());
        log.info("Cliente Carlos López → ID: {}", carlos.getId());
        log.info("  Cuenta ahorro      → ID: {}", carlosAhorro.getId());
        log.info("  Cuenta corriente   → ID: {}", carlosCorriente.getId());
    }

    private void saveMovimiento(Movimiento.Tipo tipo, String monto, String descripcion, Cuenta cuenta) {
        Movimiento m = new Movimiento(tipo, new BigDecimal(monto), descripcion, cuenta);
        movimientoRepository.save(m);
    }
}
