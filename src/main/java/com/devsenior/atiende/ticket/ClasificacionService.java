package com.devsenior.atiende.ticket;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.LectorJsonIA;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ia.dto.FacturaIA;
import com.devsenior.atiende.ticket.dto.ClasificacionResponse;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@Service
public class ClasificacionService {

    private final TicketServicio ticketServicio;
    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJsonIA lectorJsonIA;

    public ClasificacionService(
            TicketServicio ticketServicio,
            AsistenteIA asistenteIA,
            BibliotecaPrompts bibliotecaPrompts,
            LectorJsonIA lectorJsonIA) {
        this.ticketServicio = ticketServicio;
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJsonIA = lectorJsonIA;
    }

    public ClasificacionResponse clasificar(Long ticketId) {
        Ticket ticket = ticketServicio.buscarOFallar(ticketId);
        String instrucciones = bibliotecaPrompts.cargar("clasificacion");
        RespuestaIA respuesta = asistenteIA.completar(instrucciones, ticket.getMensaje());

        ClasificacionIA clasificacion = lectorJsonIA.leer(respuesta, ClasificacionIA.class);
        if (clasificacion == null || !clasificacion.estaCompleta()) {
            throw new RespuestaIaInvalidaException("La IA no devolvió una clasificación válida o completa");
        }

        String numeroPedido = apareceEnElMensaje(clasificacion.numeroPedido(), ticket.getMensaje())
                ? clasificacion.numeroPedido().trim()
                : null;

        TicketRespuesta clasificado = ticketServicio.aplicarClasificacion(ticketId, clasificacion, numeroPedido);
        return new ClasificacionResponse(clasificado, FacturaIA.de(respuesta));
    }

    private boolean apareceEnElMensaje(String numeroPedido, String mensaje) {
        if (numeroPedido == null || numeroPedido.isBlank() || mensaje == null) {
            return false;
        }
        return Pattern.compile("\\b" + Pattern.quote(numeroPedido.trim()) + "\\b").matcher(mensaje).find();
    }
}
