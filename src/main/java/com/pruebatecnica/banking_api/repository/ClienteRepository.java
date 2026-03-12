package com.pruebatecnica.banking_api.repository;

import com.pruebatecnica.banking_api.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
