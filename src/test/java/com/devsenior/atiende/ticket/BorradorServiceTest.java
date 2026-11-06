package com.devsenior.atiende.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
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
import com.devsenior.atiende.ia.LectorJsonIA;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.pedido.PedidoService;
import com.devsenior.atiende.ticket.dto.BorradorResponse;

@ExtendWith(MockitoExtension.class)
class BorradorServiceTest {

    @Mock
    private TicketServicio ticketServicio;

    @Mock
    private PedidoService pedidoService;

    @Mock
    private AsistenteIA asistenteIA;

    @Mock
    private BibliotecaPrompts bibliotecaPrompts;

    @Mock
    private LectorJsonIA lectorJsonIA;

    private BorradorService borradorService;

    @BeforeEach
    void setUp() {
        borradorService = new BorradorService(
                ticketServicio, pedidoService, asistenteIA, bibliotecaPrompts, lectorJsonIA);
    }

    @Test
    void generarBorrador_canalWhatsapp_noIncluyeAsunto() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setClienteNombre("Laura");
        ticket.setCanal(CanalTicket.WHATSAPP);
        ticket.setNumeroPedido("1045");
        ticket.setMensaje("¿Dónde está mi pedido 1045?");

        when(ticketServicio.buscarOFallar(ticketId)).thenReturn(ticket);
        when(pedidoService.datosVerificados("1045"))
                .thenReturn("Pedido #1045, Estado: ENVIADO, Transportadora: TransAndina, Guía: TA-778201 (3 días de retraso)");
        when(bibliotecaPrompts.cargar(eq("borrador-respuesta"), anyMap())).thenReturn("Instrucciones borrador");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "gemini-3.5-flash-lite", 120, 40, new BigDecimal("0.0001"), 350);
        when(asistenteIA.completar("Instrucciones borrador", ticket.getMensaje())).thenReturn(respuestaIA);

        BorradorIA borradorIA = new BorradorIA("Asunto innecesario", "Hola Laura, tu pedido va con TransAndina guía TA-778201 con 3 días de retraso.");
        when(lectorJsonIA.leer(respuestaIA, BorradorIA.class)).thenReturn(borradorIA);

        BorradorResponse response = borradorService.generarBorrador(ticketId);

        assertNotNull(response);
        assertNull(response.asunto(), "En WhatsApp el borrador no debe traer asunto");
        assertEquals("Hola Laura, tu pedido va con TransAndina guía TA-778201 con 3 días de retraso.", response.cuerpo());
        assertNotNull(response.factura());
    }

    @Test
    void generarBorrador_canalCorreo_incluyeAsunto() {
        Long ticketId = 2L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setClienteNombre("Felipe");
        ticket.setCanal(CanalTicket.CORREO);
        ticket.setNumeroPedido("1046");
        ticket.setMensaje("Confirmación de entrega");

        when(ticketServicio.buscarOFallar(ticketId)).thenReturn(ticket);
        when(pedidoService.datosVerificados("1046"))
                .thenReturn("Pedido #1046, Estado: ENTREGADO, Transportadora: Servientrega");
        when(bibliotecaPrompts.cargar(eq("borrador-respuesta"), anyMap())).thenReturn("Instrucciones borrador");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "gemini-3.5-flash-lite", 100, 30, BigDecimal.ZERO, 300);
        when(asistenteIA.completar("Instrucciones borrador", ticket.getMensaje())).thenReturn(respuestaIA);

        BorradorIA borradorIA = new BorradorIA("Estado de su pedido Grano Alto Café", "Estimado Felipe, su pedido fue entregado.");
        when(lectorJsonIA.leer(respuestaIA, BorradorIA.class)).thenReturn(borradorIA);

        BorradorResponse response = borradorService.generarBorrador(ticketId);

        assertNotNull(response);
        assertEquals("Estado de su pedido Grano Alto Café", response.asunto());
        assertEquals("Estimado Felipe, su pedido fue entregado.", response.cuerpo());
    }

    @Test
    void generarBorrador_cuerpoVacio_lanzaRespuestaIaInvalidaException() {
        Long ticketId = 3L;
        Ticket ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setClienteNombre("Ana");
        ticket.setCanal(CanalTicket.CHAT_WEB);
        ticket.setMensaje("Hola");

        when(ticketServicio.buscarOFallar(ticketId)).thenReturn(ticket);
        when(bibliotecaPrompts.cargar(eq("borrador-respuesta"), anyMap())).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "modelo", 10, 5, BigDecimal.ZERO, 100);
        when(asistenteIA.completar("Instrucciones", ticket.getMensaje())).thenReturn(respuestaIA);

        BorradorIA borradorVacio = new BorradorIA(null, "   ");
        when(lectorJsonIA.leer(respuestaIA, BorradorIA.class)).thenReturn(borradorVacio);

        assertThrows(RespuestaIaInvalidaException.class, () -> borradorService.generarBorrador(ticketId));
    }
}

