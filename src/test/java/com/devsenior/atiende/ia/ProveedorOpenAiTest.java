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

class ProveedorOpenAiTest {

    private IaProperties properties;
    private MockRestServiceServer mockServer;
    private ProveedorOpenAi proveedor;

    @BeforeEach
    void setUp() {
        properties = new IaProperties();
        properties.setTimeoutSegundos(10);
        properties.setMaxTokens(500);

        IaProperties.ProveedorConfig config = properties.getOpenai();
        config.setUrl("https://generativelanguage.googleapis.com/v1beta/openai");
        config.setModelo("gemini-3.5-flash-lite");
        config.setLlave("test-llave");
        config.setPrecioEntradaMillon(new BigDecimal("0.30"));
        config.setPrecioSalidaMillon(new BigDecimal("2.50"));

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        proveedor = new ProveedorOpenAi(properties, builder.build());
    }

    @Test
    void nombre_retornaOpenai() {
        assertEquals("openai", proveedor.nombre());
    }

    @Test
    void completar_respuestaExitosa_retornaRespuestaIA() {
        String jsonRespuesta = """
                {
                  "id": "chatcmpl-123",
                  "model": "gemini-3.5-flash-lite",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "MOLESTO"
                      },
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 20,
                    "total_tokens": 120
                  }
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-llave"))
                .andRespond(withSuccess(jsonRespuesta, MediaType.APPLICATION_JSON));

        RespuestaIA respuesta = proveedor.completar("Instrucciones del sistema", "Mensaje del cliente");

        mockServer.verify();
        assertNotNull(respuesta);
        assertEquals("MOLESTO", respuesta.texto());
        assertEquals("openai", respuesta.proveedor());
        assertEquals("gemini-3.5-flash-lite", respuesta.modelo());
        assertEquals(100, respuesta.tokensEntrada());
        assertEquals(20, respuesta.tokensSalida());
        assertNotNull(respuesta.costoEstimado());
    }

    @Test
    void completar_respuestaVacia_lanzaExcepcion() {
        String jsonRespuesta = """
                {
                  "id": "chatcmpl-123",
                  "model": "gemini-3.5-flash-lite",
                  "choices": []
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(jsonRespuesta, MediaType.APPLICATION_JSON));

        assertThrows(IllegalStateException.class, () -> proveedor.completar("Instrucciones", "Mensaje"));
        mockServer.verify();
    }
}

