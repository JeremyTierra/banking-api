package com.pruebatecnica.banking_api.dto;

import com.pruebatecnica.banking_api.domain.Cuenta;

import java.math.BigDecimal;

public record CuentaResumenResponse(
        Long id,
        String numeroCuenta,
        String tipo,
        BigDecimal saldo
) {
    public static CuentaResumenResponse from(Cuenta c) {
        return new CuentaResumenResponse(
                c.getId(),
                c.getNumeroCuenta(),
                c.getTipo().name(),
                c.getSaldo()
        );
    }
}
