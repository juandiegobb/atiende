package com.devsenior.atiende.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.IaNoDisponibleException;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ticket.dto.SentimientoRespuesta;

@ExtendWith(MockitoExtension.class)
class SentimientoServiceTest {

    @Mock
    private AsistenteIA asistenteIA;

    @Mock
    private BibliotecaPrompts bibliotecaPrompts;

    private SentimientoService sentimientoService;

    @BeforeEach
    void setUp() {
        sentimientoService = new SentimientoService(asistenteIA, bibliotecaPrompts);
    }

    @Test
    void analizar_mensajeValido_retornaSentimientoRespuesta() {
        String mensaje = "Mi pedido llegó retrasado y el paquete estaba abierto";
        when(bibliotecaPrompts.cargar("sentimiento")).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA(
                "MOLESTO",
                "openai",
                "gemini-3.5-flash-lite",
                100,
                2,
                new BigDecimal("0.000030"),
                350);
        when(asistenteIA.completar("Instrucciones", mensaje)).thenReturn(respuestaIA);

        SentimientoRespuesta resultado = sentimientoService.analizar(mensaje);

        assertNotNull(resultado);
        assertEquals(SentimientoTicket.MOLESTO, resultado.sentimiento());
        assertEquals("openai", resultado.proveedor());
        verify(bibliotecaPrompts).cargar("sentimiento");
        verify(asistenteIA).completar("Instrucciones", mensaje);
    }

    @Test
    void analizar_respuestaIaInvalida_lanzaRespuestaIaInvalidaException() {
        String mensaje = "Hola";
        when(bibliotecaPrompts.cargar("sentimiento")).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA(
                "DESCONOCIDO",
                "openai",
                "gemini-3.5-flash-lite",
                50,
                2,
                BigDecimal.ZERO,
                200);
        when(asistenteIA.completar("Instrucciones", mensaje)).thenReturn(respuestaIA);

        assertThrows(RespuestaIaInvalidaException.class, () -> sentimientoService.analizar(mensaje));
    }

    @Test
    void analizar_iaNoDisponible_propagaIaNoDisponibleException() {
        String mensaje = "Mensaje";
        when(bibliotecaPrompts.cargar("sentimiento")).thenReturn("Instrucciones");
        when(asistenteIA.completar("Instrucciones", mensaje))
                .thenThrow(new IaNoDisponibleException("IA caída"));

        assertThrows(IaNoDisponibleException.class, () -> sentimientoService.analizar(mensaje));
    }
}

