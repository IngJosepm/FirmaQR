package com.davisignbridge.backend.service;

import com.davisignbridge.backend.domain.Documento;
import com.davisignbridge.backend.domain.EstadoFirmante;
import com.davisignbridge.backend.domain.Firmante;
import com.davisignbridge.backend.repository.DocumentoRepository;
import com.davisignbridge.backend.repository.FirmanteRepository;
import com.davisignbridge.backend.web.dto.DocumentoResumenDTO;
import com.davisignbridge.backend.web.dto.FirmanteResumenDTO;
import com.davisignbridge.backend.web.dto.SesionDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orquesta el flujo de firma multidispositivo. La firma es SECUENCIAL por
 * diseño: solo el firmante con el orden más bajo que aún está PENDIENTE puede
 * firmar, porque cada firma es incremental sobre el PDF del firmante anterior
 * (si dos personas firmaran en paralelo sobre la misma versión, una de las
 * firmas se perdería). Esto evita tener que resolver conflictos de versiones.
 */
@Service
@Transactional
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final FirmanteRepository firmanteRepository;
    private final String baseUrl;

    public DocumentoService(DocumentoRepository documentoRepository,
                             FirmanteRepository firmanteRepository,
                             @Value("${app.base-url}") String baseUrl) {
        this.documentoRepository = documentoRepository;
        this.firmanteRepository = firmanteRepository;
        this.baseUrl = baseUrl;
    }

    public Documento crear(String nombreArchivo, byte[] pdfOriginal, List<String> correos) {
        if (correos == null || correos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debes indicar al menos un correo firmante");
        }

        Documento documento = new Documento();
        documento.setNombreArchivo(nombreArchivo);
        documento.setPdfActual(pdfOriginal);

        int orden = 0;
        for (String correo : correos) {
            Firmante firmante = new Firmante();
            firmante.setDocumento(documento);
            firmante.setCorreo(correo.trim());
            firmante.setOrden(orden++);
            documento.getFirmantes().add(firmante);
        }
        documento.recalcularEstado();
        return documentoRepository.save(documento);
    }

    @Transactional(readOnly = true)
    public Documento obtener(UUID documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Documento> listar() {
        return documentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Firmante obtenerPorToken(UUID token) {
        return firmanteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));
    }

    /**
     * Registra la firma de {@code firmante} con el PDF ya firmado que subió la
     * app. Solo se acepta si es su turno (ver clase de doc arriba).
     */
    public Documento registrarFirma(UUID token, byte[] pdfFirmado) {
        Firmante firmante = obtenerPorToken(token);
        Documento documento = firmante.getDocumento();

        Firmante enTurno = documento.firmanteEnTurno();
        if (enTurno == null || !enTurno.getId().equals(firmante.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Todavía no es el turno de este firmante (o el documento ya se firmó)");
        }

        documento.setPdfActual(pdfFirmado);
        firmante.setEstado(EstadoFirmante.FIRMADO);
        firmante.setFirmadoEn(Instant.now());
        documento.recalcularEstado();

        return documentoRepository.save(documento);
    }

    /** URL que se codifica en el QR: lo único que necesita la app para arrancar la sesión. */
    public String urlSesion(UUID token) {
        return baseUrl + "/api/sesion/" + token;
    }

    public SesionDTO aSesionDTO(Firmante firmante) {
        Documento documento = firmante.getDocumento();
        Firmante enTurno = documento.firmanteEnTurno();
        boolean tuTurno = enTurno != null && enTurno.getId().equals(firmante.getId());

        return new SesionDTO(
                documento.getId(),
                documento.getNombreArchivo(),
                documento.getEstado(),
                tuTurno,
                firmante.getOrden(),
                baseUrl + "/api/sesion/" + firmante.getToken() + "/pdf",
                baseUrl + "/api/sesion/" + firmante.getToken() + "/firmar"
        );
    }

    public DocumentoResumenDTO aResumenDTO(Documento documento) {
        List<FirmanteResumenDTO> firmantes = documento.getFirmantes().stream()
                .map(f -> new FirmanteResumenDTO(
                        f.getId(),
                        f.getCorreo(),
                        f.getOrden(),
                        f.getEstado(),
                        f.getFirmadoEn(),
                        baseUrl + "/api/sesion/" + f.getToken() + "/qr",
                        baseUrl + "/firmar.html?token=" + f.getToken()
                ))
                .toList();

        return new DocumentoResumenDTO(
                documento.getId(),
                documento.getNombreArchivo(),
                documento.getEstado(),
                documento.getCreadoEn(),
                firmantes
        );
    }
}
