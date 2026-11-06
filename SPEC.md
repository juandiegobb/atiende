# SPEC · Atiende · Fase 1: recepción de tickets

## Problema
Grano Alto Café (tienda online de café de especialidad) recibe mensajes por WhatsApp, correo y chat web.
Dos personas los leen a mano en tres lugares distintos: lo urgente se pierde y nadie sabe cuántos mensajes siguen sin respuesta.

## Objetivo de la fase 1
Centralizar todos los mensajes como tickets con estado, para saber qué falta por atender.

## Fuera de alcance (llega después)
- IA: clasificación, prioridad automática y respuestas sugeridas.
- Pedidos de la tienda.
- Autenticación.
- Conexión real con WhatsApp o correo: el canal se simula con peticiones HTTP.

## Entidad Ticket

| Campo | Tipo | Regla |
|---|---|---|
| id | Long | Autogenerado |
| clienteNombre | Texto | Obligatorio, máximo 100 |
| clienteContacto | Texto | Obligatorio (teléfono o correo), máximo 120 |
| canal | WHATSAPP, CORREO, CHAT_WEB | Obligatorio |
| mensaje | Texto | Obligatorio, máximo 2000 |
| estado | ABIERTO, EN_PROCESO, RESUELTO, CERRADO | Lo asigna el sistema |
| prioridad | BAJA, MEDIA, ALTA, URGENTE | Lo asigna el sistema |
| fechaCreacion | Fecha y hora | Automática |
| fechaActualizacion | Fecha y hora | Automática, cambia en cada modificación |

## Reglas de negocio
1. Todo ticket nuevo nace ABIERTO y con prioridad MEDIA. El cliente no puede enviar estado ni prioridad.
2. Transiciones de estado permitidas:

| Desde | Hacia | Caso real |
|---|---|---|
| ABIERTO | EN_PROCESO | Alguien de soporte toma el ticket |
| ABIERTO | CERRADO | Spam o mensaje duplicado |
| EN_PROCESO | RESUELTO | Se respondió al cliente |
| RESUELTO | CERRADO | El cliente confirmó la solución |
| RESUELTO | EN_PROCESO | El cliente dice que no quedó resuelto |

   Cualquier otra transición se rechaza. CERRADO es un estado final.
3. La regla de transiciones vive en el enum `EstadoTicket`, en el método `boolean puedeCambiarA(EstadoTicket destino)`.

## Endpoints

| Método | Ruta | Respuestas |
|---|---|---|
| POST | /api/tickets | 201 ticket creado · 400 datos inválidos |
| GET | /api/tickets?estado= | 200 lista, más recientes primero; el filtro es opcional |
| GET | /api/tickets/{id} | 200 · 404 |
| PATCH | /api/tickets/{id}/estado | 200 · 400 · 404 · 409 transición no permitida |

## Formato de error
JSON con: `codigo`, `error`, `mensaje`, `fecha`.

## Criterios de aceptación
- [ ] Crear un ticket devuelve 201 con estado ABIERTO y prioridad MEDIA.
- [ ] Si el cliente envía estado o prioridad, se ignoran.
- [ ] Enviar el mensaje vacío devuelve 400 indicando el campo.
- [ ] Buscar un id inexistente devuelve 404.
- [ ] ABIERTO → EN_PROCESO devuelve 200 y la respuesta trae la fechaActualizacion nueva.
- [ ] ABIERTO → RESUELTO devuelve 409.
- [ ] Filtrar con estado=ABIERTO solo devuelve tickets abiertos.

# SPEC · Atiende · Fase 2: la IA entra al producto

## Problema
El equipo de soporte no sabe qué clientes están molestos hasta leer cada mensaje.
Antes de automatizar más (Fase 3), necesitamos que Atiende pueda hablar con un modelo de IA de forma confiable, segura y con costo conocido.

## Objetivo de la fase 2
Detectar el sentimiento de un ticket (MOLESTO, NEUTRAL, CONTENTO) usando IA, con respaldo automático si el proveedor principal falla.

## Fuera de alcance (llega después)
- Guardar el sentimiento en la base de datos.
- Clasificación por categoría, prioridad automática, borradores de respuesta y chatbot.
- Autenticación (deuda técnica declarada: este endpoint consume créditos).

## Proveedores
| Orden | Nombre | Formato de API | Servicio en clase | Servicio en producción |
|---|---|---|---|---|
| 1 | openai | Chat Completions (OpenAI) | Gemini, plan gratuito | OpenAI |
| 2 | claude | Messages (Anthropic) | Ollama local | Claude |

## Reglas
1. Si un proveedor falla, se intenta el siguiente. Si todos fallan: 503.
2. Errores HTTP 400, 401, 403 y 404 son de configuración: se registran como ERROR. Los demás (429, 5xx, sin conexión, tiempo agotado, respuesta vacía) son temporales: WARN.
3. La respuesta del modelo se valida: si no es uno de los tres sentimientos, se responde 502.
4. Las llaves viven solo en `secrets.properties` (raíz del proyecto), nunca en el código ni en Git.
5. Cada análisis informa proveedor, modelo, tokens de entrada y salida, costo estimado en USD y milisegundos.
6. En los logs nunca se escribe el mensaje del cliente ni una llave.
7. Límites: tiempo máximo de espera y tokens máximos de salida, configurables.

## Endpoint
| Método | Ruta | Respuestas |
|---|---|---|
| POST | /api/tickets/{id}/sentimiento | 200 análisis · 404 ticket no existe · 502 respuesta inválida de la IA · 503 IA no disponible |

## Criterios de aceptación
- [ ] Con Gemini configurado, el análisis del ticket de Laura devuelve MOLESTO y `proveedor: openai`.
- [ ] Con una llave inválida, el análisis sigue funcionando con `proveedor: claude` y el log muestra un ERROR.
- [ ] Con Ollama apagado y llave inválida, la respuesta es 503.
- [ ] Una respuesta del modelo fuera de los tres valores produce 502.
- [ ] `secrets.properties` no aparece en `git status`.

# SPEC · Atiende · Fase 3: la IA trabaja con el equipo y con el cliente

## Problema
Llegan 300 mensajes al día y el equipo tarda unos 4 minutos por ticket en leer, clasificar, buscar el pedido y redactar la respuesta: 20 horas de trabajo para 16 horas disponibles.

## Objetivo de la fase 3
1. Clasificar un ticket con IA (categoría, prioridad, sentimiento, resumen y número de pedido) y guardar el resultado.
2. Redactar un borrador de respuesta con datos verificados del pedido. Lo aprueba una persona.
3. Un chatbot para el chat web que responde preguntas frecuentes y escala a una persona cuando hace falta.

## Fuera de alcance (llega después)
- Clasificar automáticamente al crear el ticket.
- Enviar correos o mensajes de WhatsApp de verdad.
- Guardar las conversaciones del chatbot.
- Autenticación (deuda técnica declarada desde la Fase 2).

## Prompts
Viven en `src/main/resources/prompts/` como archivos `.txt`, con variables `{{nombre}}`.

| Archivo | Variables | Uso |
|---|---|---|
| sentimiento.txt | ninguna | Análisis de sentimiento (Fase 2) |
| clasificacion.txt | ninguna | Clasificación del ticket |
| borrador-respuesta.txt | cliente, formato, datos | Borrador de respuesta |
| chatbot.txt | cliente, datos | Chatbot del chat web |

## Categorías
ENVIO, PRODUCTO, PAGO, CONSULTA, OTRO.

## Reglas
1. Los prompts no se escriben dentro de clases Java.
2. El texto del cliente nunca se mezcla con las instrucciones: va siempre como mensaje aparte.
3. Toda respuesta en JSON se valida. Si no es JSON válido, le faltan campos o trae valores no permitidos: 502.
4. El número de pedido que proponga la IA solo se guarda si aparece en el mensaje del cliente.
5. Los datos del pedido (estado, transportadora, guía, fechas y días de retraso) los calcula y redacta Java. La IA solo los comunica.
6. El borrador nunca se envía solo: lo revisa una persona. Si el canal es CORREO lleva asunto; si no, no.
7. El chatbot nunca promete cupones, descuentos ni reembolsos. Si su respuesta lo hace, el cliente recibe un mensaje seguro y se escala.
8. Al escalar, el chatbot crea un ticket con canal CHAT_WEB y los mensajes del cliente.
9. El chatbot no guarda memoria: el historial llega completo en cada petición (máximo 20 turnos de 500 caracteres).

## Pedidos
Viven en el sistema de la tienda (simulado en `PedidoService`): 1045, 1046 y 1050.

## Endpoints
| Método | Ruta | Respuestas |
|---|---|---|
| POST | /api/tickets/{id}/clasificacion | 200 · 404 · 502 · 503 |
| POST | /api/tickets/{id}/borrador | 200 · 404 · 502 · 503 |
| POST | /api/chat | 200 · 400 · 502 · 503 |

## Criterios de aceptación
- [ ] Clasificar el ticket de Laura guarda categoría ENVIO, sentimiento MOLESTO, número de pedido 1045 y una prioridad más alta que MEDIA.
- [ ] Si la IA propone un número de pedido que no está en el mensaje, se guarda vacío.
- [ ] El borrador del ticket de Laura menciona TransAndina, la guía TA-778201 y los 3 días de retraso, y no trae asunto.
- [ ] El borrador de un ticket que llegó por CORREO trae asunto.
- [ ] El chatbot responde preguntas de envíos y moliendas sin crear tickets.
- [ ] Si el cliente pide hablar con una persona, la respuesta trae escalado true y el id de un ticket nuevo.
- [ ] Si la IA promete un cupón, el cliente recibe el mensaje seguro y se crea un ticket.
- [ ] Los 38 tests anteriores siguen en verde.


# SPEC · Atiende · Fase 3: la IA trabaja con el equipo y con el cliente

## Problema
Llegan 300 mensajes al día y el equipo tarda unos 4 minutos por ticket en leer, clasificar, buscar el pedido y redactar la respuesta: 20 horas de trabajo para 16 horas disponibles.

## Objetivo de la fase 3
1. Clasificar un ticket con IA (categoría, prioridad, sentimiento, resumen y número de pedido) y guardar el resultado.
2. Redactar un borrador de respuesta con datos verificados del pedido. Lo aprueba una persona.
3. Un chatbot para el chat web que responde preguntas frecuentes y escala a una persona cuando hace falta.

## Fuera de alcance (llega después)
- Clasificar automáticamente al crear el ticket.
- Enviar correos o mensajes de WhatsApp de verdad.
- Guardar las conversaciones del chatbot.
- Autenticación (deuda técnica declarada desde la Fase 2).

## Prompts
Viven en `src/main/resources/prompts/` como archivos `.txt`, con variables `{{nombre}}`.

| Archivo | Variables | Uso |
|---|---|---|
| sentimiento.txt | ninguna | Análisis de sentimiento (Fase 2) |
| clasificacion.txt | ninguna | Clasificación del ticket |
| borrador-respuesta.txt | cliente, formato, datos | Borrador de respuesta |
| chatbot.txt | cliente, datos | Chatbot del chat web |

## Categorías
ENVIO, PRODUCTO, PAGO, CONSULTA, OTRO.

## Reglas
1. Los prompts no se escriben dentro de clases Java.
2. El texto del cliente nunca se mezcla con las instrucciones: va siempre como mensaje aparte.
3. Toda respuesta en JSON se valida. Si no es JSON válido, le faltan campos o trae valores no permitidos: 502.
4. El número de pedido que proponga la IA solo se guarda si aparece en el mensaje del cliente.
5. Los datos del pedido (estado, transportadora, guía, fechas y días de retraso) los calcula y redacta Java. La IA solo los comunica.
6. El borrador nunca se envía solo: lo revisa una persona. Si el canal es CORREO lleva asunto; si no, no.
7. El chatbot nunca promete cupones, descuentos ni reembolsos. Si su respuesta lo hace, el cliente recibe un mensaje seguro y se escala.
8. Al escalar, el chatbot crea un ticket con canal CHAT_WEB y los mensajes del cliente.
9. El chatbot no guarda memoria: el historial llega completo en cada petición (máximo 20 turnos de 500 caracteres).

## Pedidos
Viven en el sistema de la tienda (simulado en `PedidoService`): 1045, 1046 y 1050.

## Endpoints
| Método | Ruta | Respuestas |
|---|---|---|
| POST | /api/tickets/{id}/clasificacion | 200 · 404 · 502 · 503 |
| POST | /api/tickets/{id}/borrador | 200 · 404 · 502 · 503 |
| POST | /api/chat | 200 · 400 · 502 · 503 |

## Criterios de aceptación
- [ ] Clasificar el ticket de Laura guarda categoría ENVIO, sentimiento MOLESTO, número de pedido 1045 y una prioridad más alta que MEDIA.
- [ ] Si la IA propone un número de pedido que no está en el mensaje, se guarda vacío.
- [ ] El borrador del ticket de Laura menciona TransAndina, la guía TA-778201 y los 3 días de retraso, y no trae asunto.
- [ ] El borrador de un ticket que llegó por CORREO trae asunto.
- [ ] El chatbot responde preguntas de envíos y moliendas sin crear tickets.
- [ ] Si el cliente pide hablar con una persona, la respuesta trae escalado true y el id de un ticket nuevo.
- [ ] Si la IA promete un cupón, el cliente recibe el mensaje seguro y se crea un ticket.
- [ ] Los 38 tests anteriores siguen en verde.