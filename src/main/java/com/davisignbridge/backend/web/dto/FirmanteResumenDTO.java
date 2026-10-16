package com.davisignbridge.backend.web.dto;

import com.davisignbridge.backend.domain.EstadoFirmante;

import java.time.Instant;
import java.util.UUID;

public record FirmanteResumenDTO(
        UUID id,
        String correo,
        int orden,
        EstadoFirmante estado,
        Instant firmadoEn,
        String qrUrl,
        String linkUrl
) {
}
