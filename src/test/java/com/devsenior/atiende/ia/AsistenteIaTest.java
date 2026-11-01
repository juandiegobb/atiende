package com.devsenior.atiende.ia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class AsistenteIaTest {

    @Mock
    private ProveedorIA proveedorOpenAi;

    @Mock
    private ProveedorIA proveedorClaude;

    private AsistenteIA asistenteIA;

    @BeforeEach
    void setUp() {
        lenient().when(proveedorOpenAi.nombre()).thenReturn("openai");
        lenient().when(proveedorClaude.nombre()).thenReturn("claude");
        asistenteIA = new AsistenteIA(List.of(proveedorOpenAi, proveedorClaude));
    }

    @Test
    void completar_primerProveedorExitoso_noLlamaAlSegundo() {
        RespuestaIA respuestaEsperada = new RespuestaIA(
                "MOLESTO", "openai", "gemini-3.5-flash-lite", 100, 10, new BigDecimal("0.0001"), 300);

        when(proveedorOpenAi.completar("instrucciones", "mensaje")).thenReturn(respuestaEsperada);

        RespuestaIA resultado = asistenteIA.completar("instrucciones", "mensaje");

        assertNotNull(resultado);
        assertEquals("MOLESTO", resultado.texto());
        assertEquals("openai", resultado.proveedor());
        verify(proveedorClaude, never()).completar(anyString(), anyString());
    }

    @Test
    void completar_primerProveedorFallaCon401_usaFallbackClaude() {
        when(proveedorOpenAi.completar("instrucciones", "mensaje"))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, null, null));

        RespuestaIA respuestaClaude = new RespuestaIA(
                "MOLESTO", "claude", "qwen3:1.7b", 80, 5, BigDecimal.ZERO, 500);
        when(proveedorClaude.completar("instrucciones", "mensaje")).thenReturn(respuestaClaude);

        RespuestaIA resultado = asistenteIA.completar("instrucciones", "mensaje");

        assertNotNull(resultado);
        assertEquals("MOLESTO", resultado.texto());
        assertEquals("claude", resultado.proveedor());
        verify(proveedorClaude).completar("instrucciones", "mensaje");
    }

    @Test
    void completar_primerProveedorFallaCon500_usaFallbackClaude() {
        when(proveedorOpenAi.completar("instrucciones", "mensaje"))
                .thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Server Error", HttpHeaders.EMPTY, null, null));

        RespuestaIA respuestaClaude = new RespuestaIA(
                "NEUTRAL", "claude", "qwen3:1.7b", 80, 5, BigDecimal.ZERO, 400);
        when(proveedorClaude.completar("instrucciones", "mensaje")).thenReturn(respuestaClaude);

        RespuestaIA resultado = asistenteIA.completar("instrucciones", "mensaje");

        assertNotNull(resultado);
        assertEquals("NEUTRAL", resultado.texto());
        assertEquals("claude", resultado.proveedor());
    }

    @Test
    void completar_primerProveedorFallaConTimeout_usaFallbackClaude() {
        when(proveedorOpenAi.completar("instrucciones", "mensaje"))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        RespuestaIA respuestaClaude = new RespuestaIA(
                "CONTENTO", "claude", "qwen3:1.7b", 80, 5, BigDecimal.ZERO, 400);
        when(proveedorClaude.completar("instrucciones", "mensaje")).thenReturn(respuestaClaude);

        RespuestaIA resultado = asistenteIA.completar("instrucciones", "mensaje");

        assertNotNull(resultado);
        assertEquals("CONTENTO", resultado.texto());
        assertEquals("claude", resultado.proveedor());
    }

    @Test
    void completar_todosFallan_lanzaIaNoDisponibleException() {
        when(proveedorOpenAi.completar("instrucciones", "mensaje"))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, null, null));
        when(proveedorClaude.completar("instrucciones", "mensaje"))
                .thenThrow(new ResourceAccessException("Connection refused"));

        assertThrows(IaNoDisponibleException.class, () -> asistenteIA.completar("instrucciones", "mensaje"));
    }
}

