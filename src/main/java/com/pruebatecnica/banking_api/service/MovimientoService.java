package com.pruebatecnica.banking_api.service;

import com.pruebatecnica.banking_api.domain.Cuenta;
import com.pruebatecnica.banking_api.domain.Movimiento;
import com.pruebatecnica.banking_api.exception.BusinessException;
import com.pruebatecnica.banking_api.repository.CuentaRepository;
import com.pruebatecnica.banking_api.repository.MovimientoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CuentaRepository cuentaRepository;

    @Transactional(readOnly = true)
    public Page<Movimiento> historial(Long cuentaId, int page, int size) {
        getCuentaOrThrow(cuentaId);
        return movimientoRepository.findByCuentaIdOrderByFechaDesc(
                cuentaId, PageRequest.of(page, size));
    }

    @Transactional
    public Movimiento depositar(Long cuentaId, BigDecimal monto, String descripcion) {
        Cuenta cuenta = getCuentaOrThrow(cuentaId);

        cuenta.setSaldo(cuenta.getSaldo().add(monto));
        cuentaRepository.save(cuenta);

        Movimiento movimiento = new Movimiento(
                Movimiento.Tipo.DEPOSITO, monto,
                descripcion != null ? descripcion : "Depósito",
                cuenta
        );
        Movimiento guardado = movimientoRepository.save(movimiento);
        log.info("Depósito registrado: cuenta={}, monto={}", cuentaId, monto);
        return guardado;
    }

    @Transactional
    public Movimiento retirar(Long cuentaId, BigDecimal monto, String descripcion) {
        Cuenta cuenta = getCuentaOrThrow(cuentaId);

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new BusinessException("Saldo insuficiente. Saldo disponible: " + cuenta.getSaldo());
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);

        Movimiento movimiento = new Movimiento(
                Movimiento.Tipo.RETIRO, monto,
                descripcion != null ? descripcion : "Retiro",
                cuenta
        );
        Movimiento guardado = movimientoRepository.save(movimiento);
        log.info("Retiro registrado: cuenta={}, monto={}", cuentaId, monto);
        return guardado;
    }

    @Transactional
    public void transferir(Long origenId, Long destinoId, BigDecimal monto, String descripcion) {
        if (origenId.equals(destinoId)) {
            throw new BusinessException("La cuenta origen y destino no pueden ser la misma");
        }

        Cuenta origen = getCuentaOrThrow(origenId);
        Cuenta destino = getCuentaOrThrow(destinoId);

        if (origen.getSaldo().compareTo(monto) < 0) {
            throw new BusinessException("Saldo insuficiente en cuenta origen. Saldo disponible: " + origen.getSaldo());
        }

        String desc = descripcion != null ? descripcion : "Transferencia";

        origen.setSaldo(origen.getSaldo().subtract(monto));
        cuentaRepository.save(origen);

        destino.setSaldo(destino.getSaldo().add(monto));
        cuentaRepository.save(destino);

        Movimiento retiro = new Movimiento(Movimiento.Tipo.TRANSFERENCIA, monto,
                desc + " → cuenta #" + destino.getNumeroCuenta(), origen);
        retiro.setCuentaContraparteId(destinoId);
        movimientoRepository.save(retiro);

        Movimiento deposito = new Movimiento(Movimiento.Tipo.TRANSFERENCIA, monto,
                desc + " ← cuenta #" + origen.getNumeroCuenta(), destino);
        deposito.setCuentaContraparteId(origenId);
        movimientoRepository.save(deposito);

        log.info("Transferencia registrada: origen={}, destino={}, monto={}", origenId, destinoId, monto);
    }

    private Cuenta getCuentaOrThrow(Long cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() -> new EntityNotFoundException("Cuenta no encontrada con id: " + cuentaId));
    }
}
