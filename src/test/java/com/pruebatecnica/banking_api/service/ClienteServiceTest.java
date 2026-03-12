package com.pruebatecnica.banking_api.service;

import com.pruebatecnica.banking_api.domain.Cliente;
import com.pruebatecnica.banking_api.domain.Cuenta;
import com.pruebatecnica.banking_api.dto.ResumenClienteResponse;
import com.pruebatecnica.banking_api.repository.ClienteRepository;
import com.pruebatecnica.banking_api.repository.CuentaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - Resumen financiero")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @InjectMocks
    private ClienteService clienteService;

    private Cliente cliente;
    private Cuenta ahorro;
    private Cuenta corriente;

    @BeforeEach
    void setUp() {
        cliente = new Cliente("Ana García", "12345678");

        ahorro = new Cuenta("0001-0001", Cuenta.Tipo.AHORRO, new BigDecimal("5000.00"), cliente);
        corriente = new Cuenta("0001-0002", Cuenta.Tipo.CORRIENTE, new BigDecimal("3000.00"), cliente);
    }

    @Test
    @DisplayName("Resumen: retorna todas las cuentas con saldo total correcto")
    void resumen_retornaCuentasYSaldoTotal() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of(ahorro, corriente));

        ResumenClienteResponse resumen = clienteService.resumen(1L);

        assertThat(resumen.nombre()).isEqualTo("Ana García");
        assertThat(resumen.documento()).isEqualTo("12345678");
        assertThat(resumen.cuentas()).hasSize(2);
        assertThat(resumen.saldoTotal()).isEqualByComparingTo("8000.00");
    }

    @Test
    @DisplayName("Resumen: cliente sin cuentas devuelve saldo total cero")
    void resumen_sinCuentas_saldoTotalCero() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(cuentaRepository.findByClienteId(1L)).thenReturn(List.of());

        ResumenClienteResponse resumen = clienteService.resumen(1L);

        assertThat(resumen.cuentas()).isEmpty();
        assertThat(resumen.saldoTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Resumen: cliente inexistente lanza EntityNotFoundException")
    void resumen_clienteNoExiste_lanzaExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.resumen(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
