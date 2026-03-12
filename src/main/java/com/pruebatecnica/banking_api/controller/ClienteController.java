package com.pruebatecnica.banking_api.controller;

import com.pruebatecnica.banking_api.dto.ResumenClienteResponse;
import com.pruebatecnica.banking_api.service.ClienteService;
import com.pruebatecnica.banking_api.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Clientes", description = "Consulta de información y resumen financiero de clientes")
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;
    private final ReportService reportService;

    @Operation(summary = "Resumen financiero del cliente",
               description = "Retorna todas las cuentas del cliente con sus saldos actuales y el saldo total consolidado")
    @GetMapping("/{id}/resumen")
    public ResponseEntity<ResumenClienteResponse> resumen(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.resumen(id));
    }

    @Operation(summary = "Resumen financiero en PDF",
               description = "Genera y descarga un reporte PDF del resumen financiero del cliente usando JasperReports")
    @GetMapping(value = "/{id}/resumen/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> resumenPdf(@PathVariable Long id) {
        byte[] pdf = reportService.generarResumenPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"resumen-cliente-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
