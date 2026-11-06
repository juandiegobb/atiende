package com.devsenior.atiende.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class LectorJsonIATest {

    private LectorJsonIA lectorJsonIA;

    @BeforeEach
    void setUp() {
        lectorJsonIA = new LectorJsonIA(new ObjectMapper());
    }

    record PruebaDto(String nombre, int valor) {}

    @Test
    void leer_jsonLimpio_deserializaCorrectamente() {
        String json = "{\"nombre\": \"Café\", \"valor\": 42}";
        RespuestaIA respuesta = new RespuestaIA(json, "openai", "modelo", 10, 5, BigDecimal.ZERO, 100);

        PruebaDto resultado = lectorJsonIA.leer(respuesta, PruebaDto.class);

        assertNotNull(resultado);
        assertEquals("Café", resultado.nombre());
        assertEquals(42, resultado.valor());
    }

    @Test
    void leer_jsonConTextoAdicionalAntesYDespues_extraeCorrectamente() {
        String texto = "Aquí está tu respuesta:\n```json\n{\"nombre\": \"Especial\", \"valor\": 99}\n```\nEspero te sirva.";
        RespuestaIA respuesta = new RespuestaIA(texto, "claude", "modelo", 20, 10, BigDecimal.ZERO, 150);

        PruebaDto resultado = lectorJsonIA.leer(respuesta, PruebaDto.class);

        assertNotNull(resultado);
        assertEquals("Especial", resultado.nombre());
        assertEquals(99, resultado.valor());
    }

    @Test
    void leer_sinJson_lanzaRespuestaIaInvalidaException() {
        RespuestaIA respuesta = new RespuestaIA("No pude procesar la solicitud", "openai", "modelo", 5, 2, BigDecimal.ZERO, 50);

        assertThrows(RespuestaIaInvalidaException.class, () -> lectorJsonIA.leer(respuesta, PruebaDto.class));
    }

    @Test
    void leer_jsonMalformado_lanzaRespuestaIaInvalidaException() {
        RespuestaIA respuesta = new RespuestaIA("{\"nombre\": \"Incompleto\", }", "openai", "modelo", 5, 2, BigDecimal.ZERO, 50);

        assertThrows(RespuestaIaInvalidaException.class, () -> lectorJsonIA.leer(respuesta, PruebaDto.class));
    }
}
