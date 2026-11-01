package com.devsenior.atiende.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProveedorClaudeTest {

    private IaProperties properties;
    private MockRestServiceServer mockServer;
    private ProveedorClaude proveedor;

    @BeforeEach
    void setUp() {
        properties = new IaProperties();
        properties.setTimeoutSegundos(10);
        properties.setMaxTokens(500);

        IaProperties.ProveedorConfig config = properties.getClaude();
        config.setUrl("http://localhost:11434");
        config.setModelo("qwen3:1.7b");
        config.setLlave("ollama");
        config.setPrecioEntradaMillon(BigDecimal.ZERO);
        config.setPrecioSalidaMillon(BigDecimal.ZERO);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        proveedor = new ProveedorClaude(properties, builder.build());
    }

    @Test
    void nombre_retornaClaude() {
        assertEquals("claude", proveedor.nombre());
    }

    @Test
    void completar_respuestaExitosa_retornaRespuestaIA() {
        String jsonRespuesta = """
                {
                  "id": "msg_123",
                  "type": "message",
                  "role": "assistant",
                  "model": "qwen3:1.7b",
                  "content": [
                    {
                      "type": "thinking",
                      "thinking": "Analizando..."
                    },
                    {
                      "type": "text",
                      "text": "CONTENTO"
                    }
                  ],
                  "stop_reason": "end_turn",
                  "usage": {
                    "input_tokens": 50,
                    "output_tokens": 5
                  }
                }
                """;

        mockServer.expect(requestTo("http://localhost:11434/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "ollama"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andRespond(withSuccess(jsonRespuesta, MediaType.APPLICATION_JSON));

        RespuestaIA respuesta = proveedor.completar("Instrucciones", "Mensaje del cliente");

        mockServer.verify();
        assertNotNull(respuesta);
        assertEquals("CONTENTO", respuesta.texto());
        assertEquals("claude", respuesta.proveedor());
        assertEquals("qwen3:1.7b", respuesta.modelo());
        assertEquals(50, respuesta.tokensEntrada());
        assertEquals(5, respuesta.tokensSalida());
        assertEquals(0, BigDecimal.ZERO.compareTo(respuesta.costoEstimado()));
    }

    @Test
    void completar_sinBloquesDeTexto_lanzaExcepcion() {
        String jsonRespuesta = """
                {
                  "id": "msg_123",
                  "type": "message",
                  "role": "assistant",
                  "model": "qwen3:1.7b",
                  "content": [
                    {
                      "type": "thinking",
                      "thinking": "Pensando..."
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("http://localhost:11434/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonRespuesta, MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> proveedor.completar("Instrucciones", "Mensaje"));
        mockServer.verify();
    }
}

