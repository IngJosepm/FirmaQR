package com.davisignbridge.backend.web;

import com.davisignbridge.backend.domain.Documento;
import com.davisignbridge.backend.service.DocumentoService;
import com.davisignbridge.backend.web.dto.DocumentoResumenDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Usado por la web de gestión documentaria (subir PDF + lista de correos, ver estado). */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public DocumentoResumenDTO crear(@RequestParam("pdf") MultipartFile pdf,
                                      @RequestParam("correos") List<String> correos) {
        byte[] bytes;
        try {
            bytes = pdf.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el PDF", e);
        }
        Documento documento = service.crear(pdf.getOriginalFilename(), bytes, correos);
        return service.obtenerResumenDTO(documento.getId());
    }

    @GetMapping
    public List<DocumentoResumenDTO> listar() {
        return service.listarResumenes();
    }

    @GetMapping("/{id}")
    public DocumentoResumenDTO obtener(@PathVariable UUID id) {
        return service.obtenerResumenDTO(id);
    }
}
