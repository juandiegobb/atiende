# Atiende · instrucciones para el agente

## Contexto
Backend de atención al cliente de Grano Alto Café.
Antes de cambiar reglas de negocio, lee `SPEC.md`: es la fuente de verdad.

## Stack
- Java 17, Spring Boot 4.1, Maven, MySQL.
- Configuración solo en `application.properties`. Nunca `application.yml` ni perfiles.

## Arquitectura
- Paquete base `com.devsenior.atiende`, organizado por dominio: `ticket/`, `compartido/`.
- Capas: Controller → Service → Repository. El Controller no tiene lógica de negocio.
- DTOs como `record` en `<dominio>/dto`. Nunca expongas entidades JPA en la API.
- Un mapper propio por dominio (`@Component`), sin librerías de mapeo.
- Inyección por constructor. Nunca `@Autowired` sobre atributos.
- Enums persistidos con `@Enumerated(EnumType.STRING)`.
- Errores con excepciones propias y un `@RestControllerAdvice` en `compartido/`.

## Código
- Clases, métodos y variables en español.
- Sin Lombok: getters y setters explícitos.
- Validación de entrada con Jakarta Validation en los DTO de request.

## Tests
- JUnit 5 + Mockito. Los tests del Service no levantan Spring.
- Nunca modifiques un test para que pase. Si un test falla, explícame si la causa está en el test o en el código y espera mi decisión.
- Un test rojo contra la SPEC es una pregunta de negocio: no la resuelvas tú.

## Límites
- No agregues dependencias al `pom.xml` sin preguntarme.
- No modifiques `application.properties` salvo que te lo pida.
- Si algo de la SPEC es ambiguo, pregunta antes de suponer.