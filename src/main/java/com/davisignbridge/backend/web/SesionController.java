package com.davisignbridge.backend.web;

import com.davisignbridge.backend.domain.Firmante;
import com.davisignbridge.backend.service.DocumentoService;
import com.davisignbridge.backend.service.QrService;
import com.davisignbridge.backend.web.dto.SesionDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.UUID;

/**
 * Lo que consume la app Android/iOS tras escanear el QR de un firmante: el QR
 * solo codifica la URL de {@code GET /api/sesion/{token}} (sin cifrados raros,
 * es nuestro propio backend) y a partir de ahí la app descarga el PDF actual,
 * lo firma con el DNIe por NFC y sube el resultado con {@code POST .../firmar}.
 */
@RestController
@RequestMapping("/api/sesion")
public class SesionController {

    private final DocumentoService service;
    private final QrService qrService;

    public SesionController(DocumentoService service, QrService qrService) {
        this.service = service;
        this.qrService = qrService;
    }

    @GetMapping("/{token}")
    public SesionDTO obtenerSesion(@PathVariable UUID token) {
        Firmante firmante = service.obtenerPorToken(token);
        return service.aSesionDTO(firmante);
    }

    @GetMapping(value = "/{token}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID token) {
        Firmante firmante = service.obtenerPorToken(token);
        byte[] pdf = firmante.getDocumento().getPdfActual();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + firmante.getDocumento().getNombreArchivo() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/{token}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> obtenerQr(@PathVariable UUID token) {
        service.obtenerPorToken(token); // valida que el token exista (404 si no)
        byte[] png = qrService.generarPng(service.urlSesion(token), 300);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @PostMapping(value = "/{token}/firmar", consumes = "multipart/form-data")
    public SesionDTO subirFirmado(@PathVariable UUID token,
                                   @RequestParam("pdf") MultipartFile pdfFirmado) {
        byte[] bytes;
        try {
            bytes = pdfFirmado.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el PDF firmado", e);
        }
        service.registrarFirma(token, bytes);
        Firmante actualizado = service.obtenerPorToken(token);
        return service.aSesionDTO(actualizado);
    }
}
