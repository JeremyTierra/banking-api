package com.pruebatecnica.banking_api.service;

import com.pruebatecnica.banking_api.domain.Cliente;
import com.pruebatecnica.banking_api.domain.Cuenta;
import com.pruebatecnica.banking_api.dto.CuentaResumenResponse;
import com.pruebatecnica.banking_api.dto.ResumenClienteResponse;
import com.pruebatecnica.banking_api.repository.ClienteRepository;
import com.pruebatecnica.banking_api.repository.CuentaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;

    @Transactional(readOnly = true)
    public ResumenClienteResponse resumen(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con id: " + clienteId));

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(clienteId);

        List<CuentaResumenResponse> cuentasDto = cuentas.stream()
                .map(CuentaResumenResponse::from)
                .toList();

        BigDecimal saldoTotal = cuentas.stream()
                .map(Cuenta::getSaldo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Resumen consultado: cliente={}, cuentas={}, saldoTotal={}", clienteId, cuentas.size(), saldoTotal);

        return new ResumenClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getDocumento(),
                cuentasDto,
                saldoTotal
        );
    }
}
