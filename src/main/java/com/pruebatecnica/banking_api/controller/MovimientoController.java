package com.pruebatecnica.banking_api.controller;

import com.pruebatecnica.banking_api.dto.DepositoRetiroRequest;
import com.pruebatecnica.banking_api.dto.MovimientoResponse;
import com.pruebatecnica.banking_api.dto.TransferenciaRequest;
import com.pruebatecnica.banking_api.service.MovimientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Movimientos", description = "Operaciones bancarias: depósitos, retiros, transferencias e historial")
@Validated
@RestController
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    @Operation(summary = "Historial de movimientos",
               description = "Retorna los movimientos de una cuenta paginados, ordenados del más reciente al más antiguo. "
                           + "Parámetros opcionales: page (default 0), size (default 20)")
    @GetMapping("/cuentas/{id}/movimientos")
    public ResponseEntity<Page<MovimientoResponse>> historial(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<MovimientoResponse> resultado = movimientoService.historial(id, page, size)
                .map(MovimientoResponse::from);
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Depositar",
               description = "Registra un depósito en la cuenta indicada y actualiza el saldo")
    @PostMapping("/cuentas/{id}/deposito")
    public ResponseEntity<MovimientoResponse> depositar(
            @PathVariable Long id,
            @Valid @RequestBody DepositoRetiroRequest request) {
        var movimiento = movimientoService.depositar(id, request.monto(), request.descripcion());
        return ResponseEntity.ok(MovimientoResponse.from(movimiento));
    }

    @Operation(summary = "Retirar",
               description = "Registra un retiro en la cuenta indicada. Falla con 400 si el saldo es insuficiente")
    @PostMapping("/cuentas/{id}/retiro")
    public ResponseEntity<MovimientoResponse> retirar(
            @PathVariable Long id,
            @Valid @RequestBody DepositoRetiroRequest request) {
        var movimiento = movimientoService.retirar(id, request.monto(), request.descripcion());
        return ResponseEntity.ok(MovimientoResponse.from(movimiento));
    }

    @Operation(summary = "Transferir entre cuentas",
               description = "Transfiere fondos entre dos cuentas. Registra un movimiento en cada cuenta para trazabilidad completa")
    @PostMapping("/transferencias")
    public ResponseEntity<Void> transferir(@Valid @RequestBody TransferenciaRequest request) {
        movimientoService.transferir(
                request.cuentaOrigenId(),
                request.cuentaDestinoId(),
                request.monto(),
                request.descripcion()
        );
        return ResponseEntity.ok().build();
    }
}
