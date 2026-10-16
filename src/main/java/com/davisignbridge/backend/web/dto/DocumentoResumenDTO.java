package com.davisignbridge.backend.web.dto;

import com.davisignbridge.backend.domain.EstadoDocumento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DocumentoResumenDTO(
        UUID id,
        String nombreArchivo,
        EstadoDocumento estado,
        Instant creadoEn,
        List<FirmanteResumenDTO> firmantes
) {
}
