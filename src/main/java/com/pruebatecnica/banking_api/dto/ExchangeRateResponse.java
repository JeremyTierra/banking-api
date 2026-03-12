package com.pruebatecnica.banking_api.dto;

import java.math.BigDecimal;

public record ExchangeRateResponse(
        BigDecimal montoOriginal,
        String monedaOrigen,
        BigDecimal montoEnUSD,
        BigDecimal tipoDeCambio
) {}
