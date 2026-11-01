package com.devsenior.atiende.ticket.dto;

import java.math.BigDecimal;

import com.devsenior.atiende.ticket.SentimientoTicket;

public record SentimientoRespuesta(
        SentimientoTicket sentimiento,
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimado,
        long milisegundos) {
}

