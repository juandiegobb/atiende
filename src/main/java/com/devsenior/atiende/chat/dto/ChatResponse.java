package com.devsenior.atiende.chat.dto;

import com.devsenior.atiende.ia.dto.FacturaIA;

public record ChatResponse(
        String respuesta,
        boolean escalado,
        Long ticketId,
        FacturaIA factura) {
}

