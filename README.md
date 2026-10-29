<<<<<<< HEAD
# Atiende · Backend de Atención al Cliente

## Qué es el proyecto
**Atiende** es el sistema backend de atención al cliente de **Grano Alto Café** (tienda online de café de especialidad). Su propósito en la Fase 1 es centralizar todos los mensajes que llegan a través de diferentes canales (WhatsApp, correo electrónico y chat web) como tickets con estado y prioridad asignados por el sistema, garantizando visibilidad de las solicitudes pendientes y un seguimiento estructurado.

---

## Requisitos
- **Java**: JDK 17 o superior (configurado con Java 25 en `pom.xml`).
- **Maven**: Maven 3.9+ o el wrapper incluido (`mvnw` / `mvnw.cmd`).
- **MySQL**: Instancia de MySQL en ejecución en el puerto `3306` con las siguientes credenciales (según `application.properties`):
  - **URL**: `jdbc:mysql://localhost:3306/atiende_db?createDatabaseIfNotExist=true`
  - **Usuario**: `root`
  - **Contraseña**: `admin`

---

## Cómo ejecutarlo

1. Asegúrate de que el servicio de MySQL esté corriendo.
2. Ejecuta el siguiente comando en la raíz del proyecto:

```bash
# En Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# En Linux / macOS
./mvnw spring-boot:run
```

La aplicación iniciará en el puerto **8080** (`http://localhost:8080`).

---

## Tabla de endpoints

La API de tickets se expone bajo la ruta base `/api/tickets`:

| Método | Ruta | Descripción | Códigos de respuesta |
|---|---|---|---|
| `POST` | `/api/tickets` | Crea un nuevo ticket. Nace con estado `ABIERTO` y prioridad `MEDIA` (el cliente no puede definirlos). | `201 Created`<br>`400 Bad Request` |
| `GET` | `/api/tickets?estado=` | Lista tickets ordenados del más reciente al más antiguo. El parámetro de consulta `estado` es opcional (`ABIERTO`, `EN_PROCESO`, `RESUELTO`, `CERRADO`). | `200 OK` |
| `GET` | `/api/tickets/{id}` | Obtiene los detalles de un ticket por su identificador. | `200 OK`<br>`404 Not Found` |
| `PATCH` | `/api/tickets/{id}/estado` | Cambia el estado de un ticket siguiendo las reglas de transición del negocio. | `200 OK`<br>`400 Bad Request`<br>`404 Not Found`<br>`409 Conflict` |

---

## Ejemplos cURL

### 1. Crear un ticket
```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "clienteNombre": "Laura Restrepo",
    "clienteContacto": "laura@example.com",
    "canal": "WHATSAPP",
    "mensaje": "Mi pedido no ha llegado a tiempo"
  }'
```

**Respuesta esperada (`201 Created`):**
```json
{
  "id": 1,
  "clienteNombre": "Laura Restrepo",
  "clienteContacto": "laura@example.com",
  "canal": "WHATSAPP",
  "mensaje": "Mi pedido no ha llegado a tiempo",
  "estado": "ABIERTO",
  "prioridad": "MEDIA",
  "fechaCreacion": "2026-10-26T14:30:00",
  "fechaActualizacion": "2026-10-26T14:30:00"
}
```

### 2. Cambiar estado de un ticket
```bash
curl -X PATCH http://localhost:8080/api/tickets/1/estado \
  -H "Content-Type: application/json" \
  -d '{
    "estado": "EN_PROCESO"
  }'
```

**Respuesta esperada (`200 OK`):**
```json
{
  "id": 1,
  "clienteNombre": "Laura Restrepo",
  "clienteContacto": "laura@example.com",
  "canal": "WHATSAPP",
  "mensaje": "Mi pedido no ha llegado a tiempo",
  "estado": "EN_PROCESO",
  "prioridad": "MEDIA",
  "fechaCreacion": "2026-10-26T14:30:00",
  "fechaActualizacion": "2026-10-26T14:35:10"
}
```

---

## Tabla de transiciones de estado

Las transiciones permitidas se gestionan en el enum `EstadoTicket`:

| Desde | Hacia | Caso real |
|---|---|---|
| `ABIERTO` | `EN_PROCESO` | Alguien de soporte toma el ticket |
| `ABIERTO` | `CERRADO` | Spam o mensaje duplicado |
| `EN_PROCESO` | `RESUELTO` | Se respondió al cliente |
| `RESUELTO` | `CERRADO` | El cliente confirmó la solución |
| `RESUELTO` | `EN_PROCESO` | El cliente dice que no quedó resuelto |

> **Regla de negocio:** Cualquier otra transición no descrita en la tabla se rechaza con código HTTP `409 Conflict`. El estado `CERRADO` es un estado final y no permite transiciones a ningún otro estado.

---

## Formato de error

Todas las respuestas de error siguen una estructura JSON unificada con cuatro campos:

```json
{
  "codigo": 409,
  "error": "TRANSICION_NO_PERMITIDA",
  "mensaje": "No se permite cambiar el estado de ABIERTO a RESUELTO",
  "fecha": "2026-10-26T14:32:00.123456"
}
```

- `codigo` (número): Código numérico de estado HTTP (`400`, `404`, `409`).
- `error` (texto): Tipo de error estandarizado (`DATOS_INVALIDOS`, `PETICION_MALFORMADA`, `NO_ENCONTRADO`, `TRANSICION_NO_PERMITIDA`).
- `mensaje` (texto): Explicación detallada del error o del campo inválido.
- `fecha` (fecha y hora): Marca temporal en formato ISO-8601 del momento en que ocurrió la excepción.

---

## Cómo correr los tests

El proyecto incluye tests unitarios con JUnit 5 y Mockito sin levantar el contexto de Spring (`EstadoTicketTest`, `TicketServiceTest`):

```bash
# En Windows (PowerShell / CMD)
.\mvnw.cmd test

# En Linux / macOS
./mvnw test
```
=======
# atiende
un proyecto educativo de una tienda de café automatizada con IA
>>>>>>> 9cbad5bb9bf53edcc59cd2c1111ed447cd9ebfa9
