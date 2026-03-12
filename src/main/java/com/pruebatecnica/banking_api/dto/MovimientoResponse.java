package com.pruebatecnica.banking_api.dto;

import com.pruebatecnica.banking_api.domain.Movimiento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        String tipo,
        BigDecimal monto,
        String descripcion,
        LocalDateTime fecha,
        Long cuentaContraparteId
) {
    public static MovimientoResponse from(Movimiento m) {
        return new MovimientoResponse(
                m.getId(),
                m.getTipo().name(),
                m.getMonto(),
                m.getDescripcion(),
                m.getFecha(),
                m.getCuentaContraparteId()
        );
    }
}
