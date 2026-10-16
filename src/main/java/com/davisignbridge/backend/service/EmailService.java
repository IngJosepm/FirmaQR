package com.davisignbridge.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Avisa por correo a un firmante que tiene un documento pendiente de firmar,
 * con el QR incrustado para que lo escanee con la app DaviSign Bridge. Un
 * fallo de envío (sin internet, SMTP caído, etc.) se registra pero NUNCA
 * debe tumbar la creación del documento -- por eso nunca propaga la excepción.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final QrService qrService;

    public EmailService(JavaMailSender mailSender, QrService qrService) {
        this.mailSender = mailSender;
        this.qrService = qrService;
    }

    public void enviarQrAFirmante(String correo, String nombreArchivo, String urlSesion) {
        try {
            byte[] qrPng = qrService.generarPng(urlSesion, 300);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setTo(correo);
            helper.setSubject("Tienes un documento para firmar: " + nombreArchivo);
            helper.setText("""
                    <p>Te han enviado el documento <b>%s</b> para firmar con tu DNIe.</p>
                    <p>Para firmarlo:</p>
                    <ol>
                      <li>Abre la app <b>DaviSign Bridge</b> en tu celular.</li>
                      <li>Escanea este código QR (se puede escanear directo desde esta imagen, o tomando una captura/foto de ella):</li>
                    </ol>
                    <img src="cid:qrFirma" alt="Código QR para firmar" />
                    <p style="color:#888;font-size:12px;">Si no esperabas este correo, puedes ignorarlo.</p>
                    """.formatted(nombreArchivo), true);
            helper.addInline("qrFirma", new ByteArrayResource(qrPng), "image/png");

            mailSender.send(mensaje);
            log.info("Correo de firma enviado a {}", correo);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo a {}: {}", correo, e.getMessage(), e);
        }
    }
}
