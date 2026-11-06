package com.devsenior.atiende.chat;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.devsenior.atiende.chat.dto.ChatRequest;
import com.devsenior.atiende.chat.dto.ChatResponse;
import com.devsenior.atiende.chat.dto.TurnoDto;
import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.LectorJsonIA;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ia.dto.FacturaIA;
import com.devsenior.atiende.pedido.PedidoService;
import com.devsenior.atiende.ticket.CanalTicket;
import com.devsenior.atiende.ticket.TicketServicio;
import com.devsenior.atiende.ticket.dto.CrearTicketSolicitud;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@Service
public class ChatbotService {

    private static final Pattern PATRON_PROMESAS_PROHIBIDAS = Pattern.compile(
            "(?i)(cup[oó]n|descuento|reembols)");

    private static final String MENSAJE_SEGURO =
            "Una persona de nuestro equipo revisará su solicitud y lo contactará pronto.";

    private final TicketServicio ticketServicio;
    private final PedidoService pedidoService;
    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJsonIA lectorJsonIA;

    public ChatbotService(
            TicketServicio ticketServicio,
            PedidoService pedidoService,
            AsistenteIA asistenteIA,
            BibliotecaPrompts bibliotecaPrompts,
            LectorJsonIA lectorJsonIA) {
        this.ticketServicio = ticketServicio;
        this.pedidoService = pedidoService;
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJsonIA = lectorJsonIA;
    }

    public ChatResponse conversar(ChatRequest request) {
        String datosVerificados = request.numeroPedido() != null
                ? pedidoService.datosVerificados(request.numeroPedido())
                : null;
        String datos = datosVerificados != null ? datosVerificados : "No hay datos verificados para este pedido.";

        Map<String, String> variables = Map.of(
                "cliente", request.clienteNombre(),
                "datos", datos);

        String instrucciones = bibliotecaPrompts.cargar("chatbot", variables);
        String mensajeHistorial = formatearHistorial(request);

        RespuestaIA respuestaIA = asistenteIA.completar(instrucciones, mensajeHistorial);
        ChatbotIA chatbot = lectorJsonIA.leer(respuestaIA, ChatbotIA.class);

        if (chatbot == null || !chatbot.estaCompleta()) {
            throw new RespuestaIaInvalidaException("La IA no devolvió una respuesta válida para el chatbot");
        }

        boolean escalar = Boolean.TRUE.equals(chatbot.escalar());
        String respuestaTexto = chatbot.respuesta();

        if (PATRON_PROMESAS_PROHIBIDAS.matcher(respuestaTexto).find()) {
            escalar = true;
            respuestaTexto = MENSAJE_SEGURO;
        }

        Long ticketId = null;
        if (escalar) {
            String mensajeCliente = extraerMensajesCliente(request);
            TicketRespuesta ticketCreado = ticketServicio.crear(new CrearTicketSolicitud(
                    request.clienteNombre(),
                    request.clienteContacto(),
                    CanalTicket.CHAT_WEB,
                    mensajeCliente));
            ticketId = ticketCreado.id();
        }

        return new ChatResponse(respuestaTexto, escalar, ticketId, FacturaIA.de(respuestaIA));
    }

    private String formatearHistorial(ChatRequest request) {
        return request.historial().stream()
                .map(this::formatearTurno)
                .collect(Collectors.joining("\n"));
    }

    private String formatearTurno(TurnoDto turno) {
        String prefijo = turno.rol() == RolTurno.CLIENTE ? "Cliente: " : "Grano: ";
        return prefijo + turno.texto();
    }

    private String extraerMensajesCliente(ChatRequest request) {
        String mensajes = request.historial().stream()
                .filter(turno -> turno.rol() == RolTurno.CLIENTE)
                .map(TurnoDto::texto)
                .collect(Collectors.joining("\n"));

        if (mensajes.isBlank()) {
            mensajes = "Conversación de chat web con el cliente.";
        }

        if (mensajes.length() > 2000) {
            mensajes = mensajes.substring(0, 2000);
        }

        return mensajes;
    }
}

