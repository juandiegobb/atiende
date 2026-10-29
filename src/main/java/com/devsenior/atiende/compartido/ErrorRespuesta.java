package com.devsenior.atiende.compartido;

import java.time.LocalDateTime;

public record ErrorRespuesta(
        int codigo,
        String error,
        String mensaje,
        LocalDateTime fecha) {
}
