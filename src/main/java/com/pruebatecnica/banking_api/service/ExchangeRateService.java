package com.pruebatecnica.banking_api.service;

import com.pruebatecnica.banking_api.dto.ExchangeRateResponse;
import com.pruebatecnica.banking_api.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final WebClient webClient;

    @Value("${frankfurter.api.url}")
    private String frankfurterApiUrl;

    public ExchangeRateResponse convert(BigDecimal monto, String moneda) {
        if (moneda.equalsIgnoreCase("USD")) {
            return new ExchangeRateResponse(monto, "USD", monto, BigDecimal.ONE);
        }

        FrankfurterResponse response = webClient.get()
                .uri(frankfurterApiUrl + "?base={base}&symbols=USD", moneda.toUpperCase())
                .retrieve()
                .bodyToMono(FrankfurterResponse.class)
                .onErrorMap(ex -> new BusinessException("No se pudo obtener el tipo de cambio para: " + moneda + ". Verifique que sea una moneda válida."))
                .block();

        if (response == null || response.rates() == null || !response.rates().containsKey("USD")) {
            throw new BusinessException("No se encontró tipo de cambio USD para la moneda: " + moneda);
        }

        BigDecimal tipoCambio = response.rates().get("USD");
        BigDecimal montoUSD = monto.multiply(tipoCambio).setScale(4, RoundingMode.HALF_UP);

        return new ExchangeRateResponse(monto, moneda.toUpperCase(), montoUSD, tipoCambio);
    }

    // Proyección interna para deserializar la respuesta de Frankfurter
    record FrankfurterResponse(String base, String date, Map<String, BigDecimal> rates) {}
}
