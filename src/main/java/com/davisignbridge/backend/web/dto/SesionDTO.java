package com.davisignbridge.backend.web.dto;

import com.davisignbridge.backend.domain.EstadoDocumento;

import java.util.UUID;

/** Lo que ve la app (Android/iOS) al escanear el QR de un firmante. */
public record SesionDTO(
        UUID documentoId,
        String nombreArchivo,
        EstadoDocumento estadoDocumento,
        boolean tuTurno,
        int ordenFirmante,
        String pdfUrl,
        String subirFirmaUrl
) {
}
