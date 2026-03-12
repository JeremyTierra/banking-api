package com.pruebatecnica.banking_api.repository;

import com.pruebatecnica.banking_api.domain.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    Page<Movimiento> findByCuentaIdOrderByFechaDesc(Long cuentaId, Pageable pageable);
}
