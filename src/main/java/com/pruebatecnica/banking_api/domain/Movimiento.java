package com.pruebatecnica.banking_api.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos")
@Getter
@Setter
@NoArgsConstructor
public class Movimiento {

    public enum Tipo { DEPOSITO, RETIRO, TRANSFERENCIA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal monto;

    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Cuenta cuenta;

    // Referencia a la cuenta contraparte en transferencias (solo informativo)
    private Long cuentaContraparteId;

    @PrePersist
    public void prePersist() {
        this.fecha = LocalDateTime.now();
    }

    public Movimiento(Tipo tipo, BigDecimal monto, String descripcion, Cuenta cuenta) {
        this.tipo = tipo;
        this.monto = monto;
        this.descripcion = descripcion;
        this.cuenta = cuenta;
    }
}
