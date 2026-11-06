package com.devsenior.atiende.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devsenior.atiende.chat.dto.ChatRequest;
import com.devsenior.atiende.chat.dto.ChatResponse;
import com.devsenior.atiende.chat.dto.TurnoDto;
import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.LectorJsonIA;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.pedido.PedidoService;
import com.devsenior.atiende.ticket.CanalTicket;
import com.devsenior.atiende.ticket.EstadoTicket;
import com.devsenior.atiende.ticket.PrioridadTicket;
import com.devsenior.atiende.ticket.TicketServicio;
import com.devsenior.atiende.ticket.dto.CrearTicketSolicitud;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

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

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                ticketServicio, pedidoService, asistenteIA, bibliotecaPrompts, lectorJsonIA);
    }

    @Test
    void conversar_consultaGeneral_respondeSinEscalarYSinCrearTicket() {
        ChatRequest request = new ChatRequest(
                "Carlos",
                "+573001234567",
                null,
                List.of(new TurnoDto(RolTurno.CLIENTE, "¿Tienen molienda para prensa francesa?")));

        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("Instrucciones chatbot");

        RespuestaIA respuestaIA = new RespuestaIA(
                "{\"respuesta\": \"Sí, manejamos molienda gruesa ideal para prensa francesa.\", \"escalar\": false}",
                "openai", "gemini-3.5-flash-lite", 100, 20, new BigDecimal("0.0001"), 250);
        when(asistenteIA.completar(eq("Instrucciones chatbot"), eq("Cliente: ¿Tienen molienda para prensa francesa?")))
                .thenReturn(respuestaIA);

        ChatbotIA chatbotIA = new ChatbotIA("Sí, manejamos molienda gruesa ideal para prensa francesa.", false);
        when(lectorJsonIA.leer(respuestaIA, ChatbotIA.class)).thenReturn(chatbotIA);

        ChatResponse response = chatbotService.conversar(request);

        assertNotNull(response);
        assertEquals("Sí, manejamos molienda gruesa ideal para prensa francesa.", response.respuesta());
        assertFalse(response.escalado());
        assertNull(response.ticketId());
        assertNotNull(response.factura());
        verify(ticketServicio, never()).crear(any());
    }

    @Test
    void conversar_clientePideHablarConPersona_escalaYCreaTicket() {
        ChatRequest request = new ChatRequest(
                "Laura",
                "laura@correo.com",
                "1045",
                List.of(
                        new TurnoDto(RolTurno.CLIENTE, "Quiero hablar con una persona de soporte"),
                        new TurnoDto(RolTurno.ASISTENTE, "Con gusto"),
                        new TurnoDto(RolTurno.CLIENTE, "Es urgente sobre mi pedido")));

        when(pedidoService.datosVerificados("1045"))
                .thenReturn("Pedido #1045, Estado: ENVIADO, Transportadora: TransAndina");
        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("Instrucciones chatbot");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "gemini-3.5-flash-lite", 150, 30, BigDecimal.ZERO, 300);
        when(asistenteIA.completar(any(), any())).thenReturn(respuestaIA);

        ChatbotIA chatbotIA = new ChatbotIA("Un asesor del equipo se comunicará contigo de inmediato.", true);
        when(lectorJsonIA.leer(respuestaIA, ChatbotIA.class)).thenReturn(chatbotIA);

        TicketRespuesta ticketCreado = new TicketRespuesta(
                77L, "Laura", "laura@correo.com", CanalTicket.CHAT_WEB,
                "Quiero hablar con una persona de soporte\nEs urgente sobre mi pedido",
                EstadoTicket.ABIERTO, PrioridadTicket.MEDIA, LocalDateTime.now(), LocalDateTime.now());
        when(ticketServicio.crear(any(CrearTicketSolicitud.class))).thenReturn(ticketCreado);

        ChatResponse response = chatbotService.conversar(request);

        assertNotNull(response);
        assertTrue(response.escalado());
        assertEquals(77L, response.ticketId());
        assertEquals("Un asesor del equipo se comunicará contigo de inmediato.", response.respuesta());

        ArgumentCaptor<CrearTicketSolicitud> captor = ArgumentCaptor.forClass(CrearTicketSolicitud.class);
        verify(ticketServicio).crear(captor.capture());
        assertEquals(CanalTicket.CHAT_WEB, captor.getValue().canal());
        assertEquals("Laura", captor.getValue().clienteNombre());
        assertEquals("Quiero hablar con una persona de soporte\nEs urgente sobre mi pedido", captor.getValue().mensaje());
    }

    @Test
    void conversar_iaPrometeCuponODescuento_aplicaMensajeSeguroYEscala() {
        ChatRequest request = new ChatRequest(
                "Andrés",
                "+573100000000",
                null,
                List.of(new TurnoDto(RolTurno.CLIENTE, "¿Me pueden regalar un descuento?")));

        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("Instrucciones chatbot");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "gemini-3.5-flash-lite", 100, 20, BigDecimal.ZERO, 200);
        when(asistenteIA.completar(any(), any())).thenReturn(respuestaIA);

        ChatbotIA chatbotIA = new ChatbotIA("Claro, te regalamos un cupón de descuento del 15% para tu compra.", false);
        when(lectorJsonIA.leer(respuestaIA, ChatbotIA.class)).thenReturn(chatbotIA);

        TicketRespuesta ticketCreado = new TicketRespuesta(
                88L, "Andrés", "+573100000000", CanalTicket.CHAT_WEB,
                "¿Me pueden regalar un descuento?", EstadoTicket.ABIERTO, PrioridadTicket.MEDIA,
                LocalDateTime.now(), LocalDateTime.now());
        when(ticketServicio.crear(any(CrearTicketSolicitud.class))).thenReturn(ticketCreado);

        ChatResponse response = chatbotService.conversar(request);

        assertNotNull(response);
        assertTrue(response.escalado(), "Debe escalar al detectar promesa de cupón");
        assertEquals(88L, response.ticketId());
        assertEquals("Una persona de nuestro equipo revisará su solicitud y lo contactará pronto.", response.respuesta());
        assertFalse(response.respuesta().contains("15%"), "No debe prometer el cupón en la respuesta al cliente");
        verify(ticketServicio).crear(any(CrearTicketSolicitud.class));
    }

    @Test
    void conversar_respuestaIaInvalida_lanzaRespuestaIaInvalidaException() {
        ChatRequest request = new ChatRequest(
                "Sofia",
                "sofia@correo.com",
                null,
                List.of(new TurnoDto(RolTurno.CLIENTE, "Hola")));

        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("Instrucciones chatbot");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "modelo", 10, 5, BigDecimal.ZERO, 100);
        when(asistenteIA.completar(any(), any())).thenReturn(respuestaIA);

        ChatbotIA respuestaIncompleta = new ChatbotIA(null, false);
        when(lectorJsonIA.leer(respuestaIA, ChatbotIA.class)).thenReturn(respuestaIncompleta);

        assertThrows(RespuestaIaInvalidaException.class, () -> chatbotService.conversar(request));
        verify(ticketServicio, never()).crear(any());
    }

    @Test
    void conversar_mensajesClienteSuperan2000Caracteres_truncaMensaje() {
        String textoLargo = "a".repeat(2500);
        ChatRequest request = new ChatRequest(
                "Pablo",
                "pablo@correo.com",
                null,
                List.of(new TurnoDto(RolTurno.CLIENTE, textoLargo)));

        when(bibliotecaPrompts.cargar(eq("chatbot"), anyMap())).thenReturn("Instrucciones");

        RespuestaIA respuestaIA = new RespuestaIA("{}", "openai", "modelo", 10, 5, BigDecimal.ZERO, 100);
        when(asistenteIA.completar(any(), any())).thenReturn(respuestaIA);

        ChatbotIA chatbotIA = new ChatbotIA("Escalando caso.", true);
        when(lectorJsonIA.leer(respuestaIA, ChatbotIA.class)).thenReturn(chatbotIA);

        TicketRespuesta ticketCreado = new TicketRespuesta(
                99L, "Pablo", "pablo@correo.com", CanalTicket.CHAT_WEB,
                "texto", EstadoTicket.ABIERTO, PrioridadTicket.MEDIA,
                LocalDateTime.now(), LocalDateTime.now());
        when(ticketServicio.crear(any())).thenReturn(ticketCreado);

        chatbotService.conversar(request);

        ArgumentCaptor<CrearTicketSolicitud> captor = ArgumentCaptor.forClass(CrearTicketSolicitud.class);
        verify(ticketServicio).crear(captor.capture());
        assertEquals(2000, captor.getValue().mensaje().length());
    }
}

