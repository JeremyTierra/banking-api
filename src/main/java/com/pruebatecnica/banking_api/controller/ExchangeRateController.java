package com.pruebatecnica.banking_api.controller;

import com.pruebatecnica.banking_api.dto.ExchangeRateResponse;
import com.pruebatecnica.banking_api.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Tag(name = "Tipo de cambio", description = "Conversión de monedas a USD vía Frankfurter API")
@Validated
@RestController
@RequestMapping("/exchange")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @Operation(summary = "Convertir a USD",
               description = "Dado un monto y una moneda origen, retorna el equivalente en USD usando el tipo de cambio actual de Frankfurter")
    @GetMapping
    public ResponseEntity<ExchangeRateResponse> convert(
            @RequestParam
            @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
            BigDecimal amount,

            @RequestParam
            @NotBlank(message = "La moneda no puede estar vacía")
            String currency) {
        return ResponseEntity.ok(exchangeRateService.convert(amount, currency));
    }
}
