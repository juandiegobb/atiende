package com.devsenior.atiende.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatbotIA(
        String respuesta,
        Boolean escalar) {

    public boolean estaCompleta() {
        return respuesta != null && !respuesta.isBlank();
    }
}

