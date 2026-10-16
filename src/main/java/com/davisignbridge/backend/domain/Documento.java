package com.davisignbridge.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Un documento en proceso de firma multidispositivo. {@code pdfActual} se
 * reemplaza en cada firma exitosa (firma incremental: cada firmante agrega su
 * firma sobre la versión anterior, nunca se pierde una firma previa).
 */
@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "nombre_archivo", nullable = false)
    private String nombreArchivo;

    // Sin @Lob a propósito: en PostgreSQL, byte[] + @Lob mapea a "large object" (OID),
    // que no coincide con la columna BYTEA creada por Flyway. Sin @Lob, Hibernate
    // mapea byte[] directo a BYTEA, que es justamente el tipo de la migración V1.
    @Column(name = "pdf_actual", nullable = false)
    private byte[] pdfActual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumento estado = EstadoDocumento.PENDIENTE;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<Firmante> firmantes = new ArrayList<>();

    public void recalcularEstado() {
        boolean todosFirmaron = firmantes.stream()
                .allMatch(f -> f.getEstado() == EstadoFirmante.FIRMADO);
        boolean algunoFirmo = firmantes.stream()
                .anyMatch(f -> f.getEstado() == EstadoFirmante.FIRMADO);

        if (todosFirmaron && !firmantes.isEmpty()) {
            estado = EstadoDocumento.FIRMADO;
        } else if (algunoFirmo) {
            estado = EstadoDocumento.PARCIAL;
        } else {
            estado = EstadoDocumento.PENDIENTE;
        }
    }

    /** El firmante con el turno actual: el de menor orden que aún no firmó. */
    public Firmante firmanteEnTurno() {
        return firmantes.stream()
                .filter(f -> f.getEstado() == EstadoFirmante.PENDIENTE)
                .findFirst()
                .orElse(null);
    }
}
