package com.devsenior.atiende.ticket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BorradorIA(
        String asunto,
        String cuerpo) {

    public boolean estaCompleta() {
        return cuerpo != null && !cuerpo.isBlank();
    }
}

