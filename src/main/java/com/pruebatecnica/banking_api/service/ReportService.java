package com.pruebatecnica.banking_api.service;

import com.pruebatecnica.banking_api.dto.CuentaResumenResponse;
import com.pruebatecnica.banking_api.dto.ResumenClienteResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ClienteService clienteService;

    // El template se compila UNA SOLA VEZ al iniciar la aplicación.
    // Compilar en cada request sería costoso e innecesario.
    private JasperReport jasperReport;

    @PostConstruct
    public void init() {
        try {
            InputStream reportStream = getClass().getResourceAsStream("/reports/resumen_cliente.jrxml");
            if (reportStream == null) {
                throw new IllegalStateException("No se encontró el template de reporte: /reports/resumen_cliente.jrxml");
            }
            this.jasperReport = JasperCompileManager.compileReport(reportStream);
            log.info("Template JasperReports compilado correctamente");
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo compilar el template de reporte JasperReports", e);
        }
    }

    public byte[] generarResumenPdf(Long clienteId) {
        ResumenClienteResponse resumen = clienteService.resumen(clienteId);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("clienteNombre", resumen.nombre());
            params.put("clienteDocumento", resumen.documento());
            params.put("saldoTotal", resumen.saldoTotal());
            params.put("fechaGeneracion",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

            // DataSource: lista de cuentas como mapas (compatible con Java Records)
            // JRMapCollectionDataSource acepta Collection<Map<String,?>> pero no List<Map<String,Object>>
            // por la invarianza de genéricos en Java — el cast explícito resuelve esto.
            List<Map<String, ?>> rows = resumen.cuentas().stream()
                    .<Map<String, ?>>map(this::cuentaToMap)
                    .toList();
            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(rows);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, dataSource);
            byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);

            log.info("Reporte PDF generado para cliente={}", clienteId);
            return pdf;

        } catch (JRException e) {
            log.error("Error al generar reporte PDF para cliente={}: {}", clienteId, e.getMessage());
            throw new RuntimeException("Error al generar el reporte PDF", e);
        }
    }

    private Map<String, Object> cuentaToMap(CuentaResumenResponse cuenta) {
        Map<String, Object> row = new HashMap<>();
        row.put("numeroCuenta", cuenta.numeroCuenta());
        row.put("tipo", cuenta.tipo());
        row.put("saldo", cuenta.saldo());
        return row;
    }
}
