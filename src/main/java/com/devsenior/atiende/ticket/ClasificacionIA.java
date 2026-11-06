package com.devsenior.atiende.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClasificacionIA(
        Categoria categoria,
        PrioridadTicket prioridad,
        SentimientoTicket sentimiento,
        String resumen,
        String numeroPedido) {

    public boolean estaCompleta() {
        return categoria != null
                && prioridad != null
                && sentimiento != null
                && resumen != null
                && !resumen.isBlank();
    }
}

