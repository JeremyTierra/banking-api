package com.pruebatecnica.banking_api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DepositoRetiroRequest(
        @NotNull(message = "El monto es requerido")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        String descripcion
) {}
