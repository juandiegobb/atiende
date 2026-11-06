package com.devsenior.atiende.ia;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BibliotecaPromptsTest {

    private BibliotecaPrompts bibliotecaPrompts;

    @BeforeEach
    void setUp() {
        bibliotecaPrompts = new BibliotecaPrompts();
    }

    @Test
    void cargar_sinVariables_retornaContenido() {
        String resultado = bibliotecaPrompts.cargar("sentimiento");

        assertNotNull(resultado);
        assertTrue(resultado.contains("MOLESTO"));
        assertTrue(resultado.contains("NEUTRAL"));
        assertTrue(resultado.contains("CONTENTO"));
        assertFalse(resultado.contains("{{"));
    }

    @Test
    void cargar_conVariables_reemplazaCorrectamente() {
        Map<String, String> variables = Map.of(
                "cliente", "Laura Gómez",
                "datos", "Pedido entregado con 2 días de retraso"
        );

        String resultado = bibliotecaPrompts.cargar("chatbot", variables);

        assertNotNull(resultado);
        assertTrue(resultado.contains("Laura Gómez"));
        assertTrue(resultado.contains("Pedido entregado con 2 días de retraso"));
        assertFalse(resultado.contains("{{cliente}}"));
        assertFalse(resultado.contains("{{datos}}"));
        assertFalse(resultado.contains("{{"));
    }

    @Test
    void cargar_archivoInexistente_lanzaIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> bibliotecaPrompts.cargar("prompt_que_no_existe_123"));
    }

    @Test
    void cargar_variablesFaltantes_lanzaIllegalStateException() {
        // chatbot.txt requiere {{cliente}} y {{datos}}. Solo enviamos {{cliente}}.
        Map<String, String> variables = Map.of(
                "cliente", "Carlos"
        );

        assertThrows(IllegalStateException.class, () -> bibliotecaPrompts.cargar("chatbot", variables));
    }
}

