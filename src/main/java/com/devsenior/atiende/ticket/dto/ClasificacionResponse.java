package com.devsenior.atiende.ticket.dto;

import com.devsenior.atiende.ia.dto.FacturaIA;

public record ClasificacionResponse(
        TicketRespuesta ticket,
        FacturaIA factura) {
}

