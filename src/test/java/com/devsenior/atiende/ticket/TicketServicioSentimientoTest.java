package com.devsenior.atiende.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.atiende.compartido.RecursoNoEncontradoException;
import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.IaNoDisponibleException;
import com.devsenior.atiende.ia.LectorPrompts;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ticket.dto.SentimientoRespuesta;

@ExtendWith(MockitoExtension.class)
class TicketServicioSentimientoTest {

    @Mock
    private TicketRepositorio ticketRepositorio;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private AsistenteIA asistenteIA;

    @Mock
    private LectorPrompts lectorPrompts;

    private TicketServicio ticketServicio;

    @BeforeEach
    void setUp() {
        ticketServicio = new TicketServicio(ticketRepositorio, ticketMapper, asistenteIA, lectorPrompts);
    }

    @Test
    void analizarSentimiento_ticketExistenteYRespuestaValida_retornaSentimientoRespuesta() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setMensaje("Mi café llegó completamente roto y frío, pésimo servicio.");

        when(ticketRepositorio.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(lectorPrompts.leer("sentimiento")).thenReturn("Instrucciones de sentimiento");

        RespuestaIA respuestaIA = new RespuestaIA(
                "MOLESTO",
                "openai",
                "gemini-3.5-flash-lite",
                120,
                2,
                new BigDecimal("0.000041"),
                450);
        when(asistenteIA.completar("Instrucciones de sentimiento", ticket.getMensaje())).thenReturn(respuestaIA);

        SentimientoRespuesta resultado = ticketServicio.analizarSentimiento(ticketId);

        assertNotNull(resultado);
        assertEquals(SentimientoTicket.MOLESTO, resultado.sentimiento());
        assertEquals("openai", resultado.proveedor());
        assertEquals("gemini-3.5-flash-lite", resultado.modelo());
        assertEquals(120, resultado.tokensEntrada());
        assertEquals(2, resultado.tokensSalida());
        assertEquals(new BigDecimal("0.000041"), resultado.costoEstimado());
        assertEquals(450, resultado.milisegundos());

        verify(ticketRepositorio).findById(ticketId);
        verify(asistenteIA).completar("Instrucciones de sentimiento", ticket.getMensaje());
    }

    @Test
    void analizarSentimiento_ticketInexistente_lanzaRecursoNoEncontradoException() {
        when(ticketRepositorio.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> ticketServicio.analizarSentimiento(99L));
    }

    @Test
    void analizarSentimiento_respuestaIaNoReconocida_lanzaRespuestaIaInvalidaException() {
        Long ticketId = 2L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setMensaje("Hola, quisiera saber el horario de atención.");

        when(ticketRepositorio.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(lectorPrompts.leer("sentimiento")).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA(
                "INDEFINIDO",
                "openai",
                "gemini-3.5-flash-lite",
                50,
                2,
                BigDecimal.ZERO,
                200);
        when(asistenteIA.completar("Instrucciones", ticket.getMensaje())).thenReturn(respuestaIA);

        assertThrows(RespuestaIaInvalidaException.class, () -> ticketServicio.analizarSentimiento(ticketId));
    }

    @Test
    void analizarSentimiento_iaNoDisponible_propagaIaNoDisponibleException() {
        Long ticketId = 3L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setMensaje("No puedo iniciar sesión");

        when(ticketRepositorio.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(lectorPrompts.leer("sentimiento")).thenReturn("Instrucciones");
        when(asistenteIA.completar("Instrucciones", ticket.getMensaje()))
                .thenThrow(new IaNoDisponibleException("Todos los proveedores fallaron"));

        assertThrows(IaNoDisponibleException.class, () -> ticketServicio.analizarSentimiento(ticketId));
    }
}

