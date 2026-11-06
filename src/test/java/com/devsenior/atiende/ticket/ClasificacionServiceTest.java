package com.devsenior.atiende.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.atiende.compartido.RecursoNoEncontradoException;
import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.LectorJsonIA;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ticket.dto.ClasificacionResponse;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@ExtendWith(MockitoExtension.class)
class ClasificacionServiceTest {

    @Mock
    private TicketServicio ticketServicio;

    @Mock
    private AsistenteIA asistenteIA;

    @Mock
    private BibliotecaPrompts bibliotecaPrompts;

    @Mock
    private LectorJsonIA lectorJsonIA;

    private ClasificacionService clasificacionService;

    @BeforeEach
    void setUp() {
        clasificacionService = new ClasificacionService(ticketServicio, asistenteIA, bibliotecaPrompts, lectorJsonIA);
    }

    @Test
    void clasificar_conNumeroPedidoEnMensaje_guardaNumeroPedidoYClasificacion() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setClienteNombre("Laura");
        ticket.setClienteContacto("laura@correo.com");
        ticket.setCanal(CanalTicket.WHATSAPP);
        ticket.setMensaje("Hola, mi pedido 1045 tiene 3 días de retraso");

        when(ticketServicio.buscarOFallar(ticketId)).thenReturn(ticket);
        when(bibliotecaPrompts.cargar("clasificacion")).thenReturn("Instrucciones de clasificación");

        RespuestaIA respuestaIA = new RespuestaIA(
                "{\"categoria\": \"ENVIO\", \"prioridad\": \"URGENTE\", \"sentimiento\": \"MOLESTO\", \"resumen\": \"Pedido retrasado\", \"numeroPedido\": 1045}",
                "openai", "gemini-3.5-flash-lite", 100, 20, new BigDecimal("0.0001"), 300);
        when(asistenteIA.completar("Instrucciones de clasificación", ticket.getMensaje())).thenReturn(respuestaIA);

        ClasificacionIA clasificacion = new ClasificacionIA(
                Categoria.ENVIO, PrioridadTicket.URGENTE, SentimientoTicket.MOLESTO, "Pedido retrasado", "1045");
        when(lectorJsonIA.leer(respuestaIA, ClasificacionIA.class)).thenReturn(clasificacion);

        TicketRespuesta ticketActualizado = new TicketRespuesta(
                ticketId, "Laura", "laura@correo.com", CanalTicket.WHATSAPP, ticket.getMensaje(),
                EstadoTicket.ABIERTO, PrioridadTicket.URGENTE, Categoria.ENVIO, SentimientoTicket.MOLESTO,
                "Pedido retrasado", "1045", LocalDateTime.now(), LocalDateTime.now());
        when(ticketServicio.aplicarClasificacion(ticketId, clasificacion, "1045")).thenReturn(ticketActualizado);

        ClasificacionResponse response = clasificacionService.clasificar(ticketId);

        assertNotNull(response);
        assertEquals(Categoria.ENVIO, response.ticket().categoria());
        assertEquals("1045", response.ticket().numeroPedido());
        assertNotNull(response.factura());
        verify(ticketServicio).aplicarClasificacion(ticketId, clasificacion, "1045");
    }

    @Test
    void clasificar_conNumeroPedidoNoPresenteEnMensaje_descartaNumeroPedido() {
        Long ticketId = 2L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setMensaje("Mi café llegó completamente dañado");

        when(ticketServicio.buscarOFallar(ticketId)).thenReturn(ticket);
        when(bibliotecaPrompts.cargar("clasificacion")).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "modelo", 10, 5, BigDecimal.ZERO, 100);
        when(asistenteIA.completar("Instrucciones", ticket.getMensaje())).thenReturn(respuestaIA);

        ClasificacionIA clasificacion = new ClasificacionIA(
                Categoria.PRODUCTO, PrioridadTicket.ALTA, SentimientoTicket.MOLESTO, "Café dañado", "9999");
        when(lectorJsonIA.leer(respuestaIA, ClasificacionIA.class)).thenReturn(clasificacion);

        TicketRespuesta ticketActualizado = new TicketRespuesta(
                ticketId, "Carlos", "carlos@mail.com", CanalTicket.WHATSAPP, ticket.getMensaje(),
                EstadoTicket.ABIERTO, PrioridadTicket.ALTA, Categoria.PRODUCTO, SentimientoTicket.MOLESTO,
                "Café dañado", null, LocalDateTime.now(), LocalDateTime.now());
        when(ticketServicio.aplicarClasificacion(eq(ticketId), eq(clasificacion), isNull())).thenReturn(ticketActualizado);

        ClasificacionResponse response = clasificacionService.clasificar(ticketId);

        assertNotNull(response);
        assertNull(response.ticket().numeroPedido());
        verify(ticketServicio).aplicarClasificacion(ticketId, clasificacion, null);
    }

    @Test
    void clasificar_clasificacionIncompleta_lanzaRespuestaIaInvalidaException() {
        Long ticketId = 3L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setMensaje("Consulta");

        when(ticketServicio.buscarOFallar(ticketId)).thenReturn(ticket);
        when(bibliotecaPrompts.cargar("clasificacion")).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "modelo", 10, 5, BigDecimal.ZERO, 100);
        when(asistenteIA.completar("Instrucciones", ticket.getMensaje())).thenReturn(respuestaIA);

        ClasificacionIA clasificacionIncompleta = new ClasificacionIA(
                null, PrioridadTicket.BAJA, SentimientoTicket.NEUTRAL, "Consulta", null);
        when(lectorJsonIA.leer(respuestaIA, ClasificacionIA.class)).thenReturn(clasificacionIncompleta);

        assertThrows(RespuestaIaInvalidaException.class, () -> clasificacionService.clasificar(ticketId));
    }

    @Test
    void clasificar_ticketInexistente_lanzaRecursoNoEncontradoException() {
        when(ticketServicio.buscarOFallar(99L)).thenThrow(new RecursoNoEncontradoException("No existe"));

        assertThrows(RecursoNoEncontradoException.class, () -> clasificacionService.clasificar(99L));
    }
}

