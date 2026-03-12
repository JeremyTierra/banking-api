package com.pruebatecnica.banking_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
@NoArgsConstructor
public class Cuenta {

    public enum Tipo { AHORRO, CORRIENTE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroCuenta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal saldo;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    public Cuenta(String numeroCuenta, Tipo tipo, BigDecimal saldoInicial, Cliente cliente) {
        this.numeroCuenta = numeroCuenta;
        this.tipo = tipo;
        this.saldo = saldoInicial;
        this.cliente = cliente;
    }
}
