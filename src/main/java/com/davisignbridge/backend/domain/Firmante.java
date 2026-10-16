package com.davisignbridge.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "firmante")
@Getter
@Setter
@NoArgsConstructor
public class Firmante {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(nullable = false)
    private String correo;

    /** Identificador impredecible que va codificado en el QR de este firmante. */
    @Column(nullable = false, unique = true)
    private UUID token = UUID.randomUUID();

    /** Orden de firma dentro del documento (0-based). La firma es secuencial. */
    @Column(nullable = false)
    private int orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoFirmante estado = EstadoFirmante.PENDIENTE;

    @Column(name = "firmado_en")
    private Instant firmadoEn;
}
