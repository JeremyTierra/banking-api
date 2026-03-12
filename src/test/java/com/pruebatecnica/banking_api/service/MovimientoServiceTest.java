package com.pruebatecnica.banking_api.service;

import com.pruebatecnica.banking_api.domain.Cliente;
import com.pruebatecnica.banking_api.domain.Cuenta;
import com.pruebatecnica.banking_api.domain.Movimiento;
import com.pruebatecnica.banking_api.exception.BusinessException;
import com.pruebatecnica.banking_api.repository.CuentaRepository;
import com.pruebatecnica.banking_api.repository.MovimientoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MovimientoService - Lógica de negocio")
class MovimientoServiceTest {

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private MovimientoService movimientoService;

    private Cliente cliente;
    private Cuenta cuentaOrigen;
    private Cuenta cuentaDestino;

    @BeforeEach
    void setUp() {
        cliente = new Cliente("Ana García", "12345678");

        cuentaOrigen = new Cuenta("0001-0001", Cuenta.Tipo.AHORRO, new BigDecimal("1000.00"), cliente);
        cuentaOrigen.setId(1L);

        cuentaDestino = new Cuenta("0001-0002", Cuenta.Tipo.CORRIENTE, new BigDecimal("500.00"), cliente);
        cuentaDestino.setId(2L);
    }

    // ── HISTORIAL ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Historial: retorna página de movimientos de la cuenta")
    void historial_retornaPagina() {
        Movimiento m = new Movimiento(Movimiento.Tipo.DEPOSITO, new BigDecimal("100.00"), "Test", cuentaOrigen);
        Page<Movimiento> pageResult = new PageImpl<>(List.of(m), PageRequest.of(0, 20), 1);
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(movimientoRepository.findByCuentaIdOrderByFechaDesc(eq(1L), any())).thenReturn(pageResult);

        Page<Movimiento> result = movimientoService.historial(1L, 0, 20);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTipo()).isEqualTo(Movimiento.Tipo.DEPOSITO);
    }

    @Test
    @DisplayName("Historial: cuenta inexistente lanza EntityNotFoundException")
    void historial_cuentaNoExiste_lanzaExcepcion() {
        when(cuentaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.historial(99L, 0, 20))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── DEPOSITAR ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Depositar: incrementa el saldo correctamente")
    void depositar_incrementaSaldo() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.save(any())).thenReturn(cuentaOrigen);
        when(movimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        movimientoService.depositar(1L, new BigDecimal("200.00"), "Test");

        assertThat(cuentaOrigen.getSaldo()).isEqualByComparingTo("1200.00");
        verify(movimientoRepository).save(argThat(m -> m.getTipo() == Movimiento.Tipo.DEPOSITO));
    }

    @Test
    @DisplayName("Depositar: cuenta inexistente lanza EntityNotFoundException")
    void depositar_cuentaNoExiste_lanzaExcepcion() {
        when(cuentaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.depositar(99L, new BigDecimal("100.00"), null))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── RETIRAR ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Retirar: decrementa el saldo correctamente")
    void retirar_decrementaSaldo() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.save(any())).thenReturn(cuentaOrigen);
        when(movimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        movimientoService.retirar(1L, new BigDecimal("300.00"), "Test");

        assertThat(cuentaOrigen.getSaldo()).isEqualByComparingTo("700.00");
        verify(movimientoRepository).save(argThat(m -> m.getTipo() == Movimiento.Tipo.RETIRO));
    }

    @Test
    @DisplayName("Retirar: saldo insuficiente lanza BusinessException")
    void retirar_saldoInsuficiente_lanzaExcepcion() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));

        assertThatThrownBy(() -> movimientoService.retirar(1L, new BigDecimal("9999.00"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    @DisplayName("Retirar: saldo exacto permite el retiro")
    void retirar_saldoExacto_permitido() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.save(any())).thenReturn(cuentaOrigen);
        when(movimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        movimientoService.retirar(1L, new BigDecimal("1000.00"), null);

        assertThat(cuentaOrigen.getSaldo()).isEqualByComparingTo("0.00");
    }

    // ── TRANSFERIR ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Transferir: actualiza saldos de ambas cuentas")
    void transferir_actualizaAmbosSaldos() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaDestino));
        when(cuentaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(movimientoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        movimientoService.transferir(1L, 2L, new BigDecimal("400.00"), "Pago");

        assertThat(cuentaOrigen.getSaldo()).isEqualByComparingTo("600.00");
        assertThat(cuentaDestino.getSaldo()).isEqualByComparingTo("900.00");
        // Debe registrar 2 movimientos (uno por cada cuenta)
        verify(movimientoRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("Transferir: misma cuenta origen y destino lanza BusinessException")
    void transferir_mismaCuenta_lanzaExcepcion() {
        assertThatThrownBy(() -> movimientoService.transferir(1L, 1L, new BigDecimal("100.00"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("misma");
    }

    @Test
    @DisplayName("Transferir: saldo insuficiente en origen lanza BusinessException")
    void transferir_saldoInsuficiente_lanzaExcepcion() {
        when(cuentaRepository.findById(1L)).thenReturn(Optional.of(cuentaOrigen));
        when(cuentaRepository.findById(2L)).thenReturn(Optional.of(cuentaDestino));

        assertThatThrownBy(() -> movimientoService.transferir(1L, 2L, new BigDecimal("5000.00"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Saldo insuficiente");
    }

    @Test
    @DisplayName("Transferir: cuenta origen inexistente lanza EntityNotFoundException")
    void transferir_cuentaOrigenNoExiste_lanzaExcepcion() {
        when(cuentaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimientoService.transferir(99L, 2L, new BigDecimal("100.00"), null))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
