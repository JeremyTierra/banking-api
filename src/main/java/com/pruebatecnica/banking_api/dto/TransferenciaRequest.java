package com.pruebatecnica.banking_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferenciaRequest(
        @NotNull(message = "La cuenta origen es requerida")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta destino es requerida")
        Long cuentaDestinoId,

        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        String descripcion
) {}
