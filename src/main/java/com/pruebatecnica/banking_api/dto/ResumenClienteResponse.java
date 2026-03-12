package com.pruebatecnica.banking_api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenClienteResponse(
        Long clienteId,
        String nombre,
        String documento,
        List<CuentaResumenResponse> cuentas,
        BigDecimal saldoTotal
) {}
