package com.devsenior.atiende.ticket.dto;

import com.devsenior.atiende.ia.dto.FacturaIA;
import com.devsenior.atiende.ticket.CanalTicket;

public record BorradorResponse(
        Long ticketId,
        CanalTicket canal,
        String asunto,
        String cuerpo,
        FacturaIA factura) {

    public BorradorResponse(String asunto, String cuerpo, FacturaIA factura) {
        this(null, null, asunto, cuerpo, factura);
    }
}
