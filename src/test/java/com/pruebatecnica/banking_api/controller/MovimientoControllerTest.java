package com.pruebatecnica.banking_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruebatecnica.banking_api.domain.Movimiento;
import com.pruebatecnica.banking_api.exception.BusinessException;
import com.pruebatecnica.banking_api.service.MovimientoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovimientoController.class)
@DisplayName("MovimientoController - Endpoints HTTP")
class MovimientoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovimientoService movimientoService;

    // ── HISTORIAL ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /cuentas/{id}/movimientos → 200 con página de movimientos")
    void historial_retornaPaginado() throws Exception {
        Movimiento m = stubMovimiento(Movimiento.Tipo.DEPOSITO, "500.00");
        var page = new PageImpl<>(List.of(m), PageRequest.of(0, 20), 1);
        when(movimientoService.historial(eq(1L), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/cuentas/1/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].tipo").value("DEPOSITO"))
                .andExpect(jsonPath("$.content[0].monto").value(500.00))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /cuentas/{id}/movimientos → respeta parámetros page y size")
    void historial_conPaginacion_usaParametros() throws Exception {
        var page = new PageImpl<Movimiento>(List.of(), PageRequest.of(1, 5), 0);
        when(movimientoService.historial(eq(1L), eq(1), eq(5))).thenReturn(page);

        mockMvc.perform(get("/cuentas/1/movimientos?page=1&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @DisplayName("GET /cuentas/{id}/movimientos → 404 si cuenta no existe")
    void historial_cuentaNoExiste_retorna404() throws Exception {
        when(movimientoService.historial(eq(99L), anyInt(), anyInt()))
                .thenThrow(new EntityNotFoundException("Cuenta no encontrada con id: 99"));

        mockMvc.perform(get("/cuentas/99/movimientos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cuenta no encontrada con id: 99"));
    }

    // ── DEPOSITAR ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /cuentas/{id}/deposito → 200 con movimiento")
    void depositar_retornaMovimiento() throws Exception {
        when(movimientoService.depositar(eq(1L), any(), any()))
                .thenReturn(stubMovimiento(Movimiento.Tipo.DEPOSITO, "200.00"));

        mockMvc.perform(post("/cuentas/1/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("monto", 200.00))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DEPOSITO"));
    }

    @Test
    @DisplayName("POST /cuentas/{id}/deposito → 400 si monto es null")
    void depositar_montoNull_retorna400() throws Exception {
        mockMvc.perform(post("/cuentas/1/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── RETIRO ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /cuentas/{id}/retiro → 400 si saldo insuficiente")
    void retirar_saldoInsuficiente_retorna400() throws Exception {
        when(movimientoService.retirar(eq(1L), any(), any()))
                .thenThrow(new BusinessException("Saldo insuficiente. Saldo disponible: 100.00"));

        mockMvc.perform(post("/cuentas/1/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("monto", 9999.00))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Saldo insuficiente. Saldo disponible: 100.00"));
    }

    // ── TRANSFERENCIA ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /transferencias → 200 con datos válidos")
    void transferir_retorna200() throws Exception {
        doNothing().when(movimientoService).transferir(any(), any(), any(), any());

        mockMvc.perform(post("/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cuentaOrigenId", 1, "cuentaDestinoId", 2, "monto", 500.00))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /transferencias → 400 si misma cuenta origen y destino")
    void transferir_mismaCuenta_retorna400() throws Exception {
        doThrow(new BusinessException("La cuenta origen y destino no pueden ser la misma"))
                .when(movimientoService).transferir(eq(1L), eq(1L), any(), any());

        mockMvc.perform(post("/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cuentaOrigenId", 1, "cuentaDestinoId", 1, "monto", 100.00))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cuenta origen y destino no pueden ser la misma"));
    }

    @Test
    @DisplayName("POST /transferencias → 400 si monto falta en el body")
    void transferir_montoFaltante_retorna400() throws Exception {
        mockMvc.perform(post("/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("cuentaOrigenId", 1, "cuentaDestinoId", 2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Movimiento stubMovimiento(Movimiento.Tipo tipo, String monto) {
        Movimiento m = new Movimiento();
        m.setTipo(tipo);
        m.setMonto(new BigDecimal(monto));
        m.setDescripcion("Test");
        m.setFecha(LocalDateTime.now());
        return m;
    }
}
