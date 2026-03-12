package com.pruebatecnica.banking_api.repository;

import com.pruebatecnica.banking_api.domain.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {
    List<Cuenta> findByClienteId(Long clienteId);
}
