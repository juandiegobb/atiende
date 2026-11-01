package com.devsenior.atiende.ticket;

public enum SentimientoTicket {
    MOLESTO,
    NEUTRAL,
    CONTENTO;

    public static SentimientoTicket desdeTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String normalizado = texto.trim().toUpperCase();
        for (SentimientoTicket valor : values()) {
            if (valor.name().equals(normalizado)) {
                return valor;
            }
        }
        return null;
    }
}

