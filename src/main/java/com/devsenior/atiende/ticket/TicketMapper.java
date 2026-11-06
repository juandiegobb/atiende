package com.devsenior.atiende.ticket;

import org.springframework.stereotype.Component;

import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@Component
public class TicketMapper {

    public TicketRespuesta aRespuesta(Ticket ticket) {
        return new TicketRespuesta(
                ticket.getId(),
                ticket.getClienteNombre(),
                ticket.getClienteContacto(),
                ticket.getCanal(),
                ticket.getMensaje(),
                ticket.getEstado(),
                ticket.getPrioridad(),
                ticket.getCategoria(),
                ticket.getSentimiento(),
                ticket.getResumen(),
                ticket.getNumeroPedido(),
                ticket.getFechaCreacion(),
                ticket.getFechaActualizacion());
    }
}
