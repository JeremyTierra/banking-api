package com.pruebatecnica.banking_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pruebatecnica.banking_api.repository.CuentaRepository;
import com.pruebatecnica.banking_api.service.ExchangeRateService;
import com.pruebatecnica.banking_api.dto.ExchangeRateResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración: levantan el contexto completo con H2 en memoria
 * y verifican el comportamiento end-to-end de cada flujo bancario.
 *
 * ExchangeRateService se mockea para evitar dependencia de la API externa
 * en el pipeline de CI/CD.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Banking API - Tests de integración")
class BankingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CuentaRepository cuentaRepository;

    // Mockeamos solo el servicio externo; el resto usa H2 real
    @MockitoBean
    private ExchangeRateService exchangeRateService;

    // ── RESUMEN CLIENTE ────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /clientes/1/resumen → estructura y saldo total correctos")
    void resumenCliente_retornaDatosCorrectos() throws Exception {
        mockMvc.perform(get("/clientes/1/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana García"))
                .andExpect(jsonPath("$.documento").value("12345678"))
                .andExpect(jsonPath("$.cuentas").isArray())
                .andExpect(jsonPath("$.cuentas.length()").value(2))
                .andExpect(jsonPath("$.saldoTotal").value(17500.00));
    }

    @Test
    @Order(2)
    @DisplayName("GET /clientes/99/resumen → 404 si cliente no existe")
    void resumenCliente_inexistente_retorna404() throws Exception {
        mockMvc.perform(get("/clientes/99/resumen"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── HISTORIAL PAGINADO ─────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("GET /cuentas/1/movimientos → respuesta paginada con movimientos del DataInitializer")
    void historial_retornaPaginado() throws Exception {
        mockMvc.perform(get("/cuentas/1/movimientos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @Order(4)
    @DisplayName("GET /cuentas/1/movimientos?size=101 → 400 por superar el máximo permitido")
    void historial_sizeExcesivoRetorna400() throws Exception {
        mockMvc.perform(get("/cuentas/1/movimientos?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── DEPOSITAR ──────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("POST /cuentas/2/deposito → saldo se incrementa en la base de datos")
    void depositar_actualizaSaldoEnBD() throws Exception {
        BigDecimal saldoInicial = cuentaRepository.findById(2L).orElseThrow().getSaldo();

        mockMvc.perform(post("/cuentas/2/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("monto", 1000.00, "descripcion", "Bono anual"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("DEPOSITO"))
                .andExpect(jsonPath("$.monto").value(1000.00));

        BigDecimal saldoFinal = cuentaRepository.findById(2L).orElseThrow().getSaldo();
        assertThat(saldoFinal).isEqualByComparingTo(saldoInicial.add(new BigDecimal("1000.00")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /cuentas/1/deposito → 400 si monto es negativo")
    void depositar_montoNegativoRetorna400() throws Exception {
        mockMvc.perform(post("/cuentas/1/deposito")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monto\": -50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // ── RETIRAR ────────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("POST /cuentas/1/retiro → saldo se decrementa en la base de datos")
    void retirar_actualizaSaldoEnBD() throws Exception {
        BigDecimal saldoInicial = cuentaRepository.findById(1L).orElseThrow().getSaldo();

        mockMvc.perform(post("/cuentas/1/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("monto", 500.00, "descripcion", "Pago alquiler"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("RETIRO"));

        BigDecimal saldoFinal = cuentaRepository.findById(1L).orElseThrow().getSaldo();
        assertThat(saldoFinal).isEqualByComparingTo(saldoInicial.subtract(new BigDecimal("500.00")));
    }

    @Test
    @Order(8)
    @DisplayName("POST /cuentas/4/retiro → 400 si saldo insuficiente")
    void retirar_saldoInsuficienteRetorna400() throws Exception {
        mockMvc.perform(post("/cuentas/4/retiro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("monto", 99999.00))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Saldo insuficiente")));
    }

    // ── TRANSFERENCIA ──────────────────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("POST /transferencias → saldo de ambas cuentas actualizado en BD")
    void transferir_actualizaAmbasCuentasEnBD() throws Exception {
        BigDecimal origenInicial = cuentaRepository.findById(3L).orElseThrow().getSaldo();
        BigDecimal destinoInicial = cuentaRepository.findById(4L).orElseThrow().getSaldo();

        mockMvc.perform(post("/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cuentaOrigenId", 3,
                                "cuentaDestinoId", 4,
                                "monto", 200.00,
                                "descripcion", "Pago préstamo"))))
                .andExpect(status().isOk());

        BigDecimal origenFinal  = cuentaRepository.findById(3L).orElseThrow().getSaldo();
        BigDecimal destinoFinal = cuentaRepository.findById(4L).orElseThrow().getSaldo();

        assertThat(origenFinal).isEqualByComparingTo(origenInicial.subtract(new BigDecimal("200.00")));
        assertThat(destinoFinal).isEqualByComparingTo(destinoInicial.add(new BigDecimal("200.00")));
    }

    @Test
    @Order(10)
    @DisplayName("POST /transferencias → 400 si cuenta origen y destino son iguales")
    void transferir_mismaCuentaRetorna400() throws Exception {
        mockMvc.perform(post("/transferencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "cuentaOrigenId", 1,
                                "cuentaDestinoId", 1,
                                "monto", 100.00))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("misma")));
    }

    // ── EXCHANGE ───────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("GET /exchange → convierte monto correctamente usando servicio mockeado")
    void exchange_retornaConversion() throws Exception {
        when(exchangeRateService.convert(any(BigDecimal.class), anyString()))
                .thenReturn(new ExchangeRateResponse(
                        new BigDecimal("100"), "EUR",
                        new BigDecimal("108.2500"), new BigDecimal("1.0825")));

        mockMvc.perform(get("/exchange?amount=100&currency=EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monedaOrigen").value("EUR"))
                .andExpect(jsonPath("$.montoEnUSD").value(108.25));
    }

    @Test
    @Order(12)
    @DisplayName("GET /exchange → 400 si currency está vacío")
    void exchange_currencyVacioRetorna400() throws Exception {
        mockMvc.perform(get("/exchange?amount=100&currency="))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
