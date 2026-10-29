# FirmaQR (DaviSign Bridge) – Backend

Backend de una plataforma de firma digital multidispositivo con DNIe. Desde
la web de gestión se sube un PDF junto con los correos de los firmantes; cada
uno recibe por correo un QR de sesión. Al escanearlo, la app Android descarga
el documento, lo firma con el DNIe por NFC (PAdES) y lo sube de vuelta. La web
muestra el estado de cada documento en tiempo real.

## Stack

Java 17 · Spring Boot 3.5 · Spring Web · Spring Data JPA · Spring Mail ·
Bean Validation · PostgreSQL 16 · Flyway · ZXing · Lombok · Docker · Maven

## Por qué la firma es secuencial (no en paralelo)

Cada firma se agrega de forma **incremental** sobre el PDF anterior (no se
generan copias separadas). Si dos firmantes pudieran firmar al mismo tiempo
sobre la misma versión, una de las dos firmas se perdería. Por eso el backend
solo deja firmar al firmante de menor `orden` que aún esté `PENDIENTE`.

## Cómo levantar el entorno de desarrollo

```bash
docker compose up -d        # levanta PostgreSQL en localhost:15432
./mvnw spring-boot:run      # requiere JAVA_HOME apuntando a un JDK 17+
```

Backend arriba en `http://localhost:8080`. La web de gestión documentaria
está servida en la raíz (`/`).

### Configuración

- **Correo (SMTP):** las credenciales no están en el repositorio. Crea un
  archivo `secrets.properties` en la raíz (ignorado por git) con:

  ```properties
  MAIL_USERNAME=tu-correo@gmail.com
  MAIL_PASSWORD=tu-contrasena-de-aplicacion
  ```

- **`app.base-url`** (en `application.properties`): URL pública del backend
  que se codifica en el QR. Debe ser la IP de la PC en la red local (no
  `localhost`), porque el QR lo abre el celular.

## Flujo de la API

1. `POST /api/documentos` (multipart: `pdf`, `correos` repetido por cada
   firmante) → crea el documento y un `Firmante` por correo, en el orden
   recibido.
2. `GET /api/documentos` / `GET /api/documentos/{id}` → estado del documento
   y de cada firmante (para la web, con polling).
3. El QR de cada firmante (`GET /api/sesion/{token}/qr`) codifica
   `GET /api/sesion/{token}` — eso es lo único que la app necesita escanear.
4. `GET /api/sesion/{token}` → la app consulta si es su turno y de dónde
   descargar el PDF actual.
5. `GET /api/sesion/{token}/pdf` → descarga el PDF actual (ya con las firmas
   previas, si las hay).
6. `POST /api/sesion/{token}/firmar` (multipart: `pdf`) → sube el PDF firmado;
   el backend valida que sea su turno, actualiza el documento y recalcula el
   estado (`PENDIENTE` → `PARCIAL` → `FIRMADO` cuando todos firmaron).

## Próximos pasos

- Autenticación de la web de gestión (Spring Security + JWT).
- Deploy en una VM (p. ej. Oracle Cloud Always Free) con PostgreSQL gestionado.
