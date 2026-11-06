package com.devsenior.atiende.chat.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio")
        @Size(max = 100, message = "El nombre del cliente no puede superar 100 caracteres")
        String clienteNombre,

        @NotBlank(message = "El contacto del cliente es obligatorio")
        @Size(max = 120, message = "El contacto del cliente no puede superar 120 caracteres")
        String clienteContacto,

        String numeroPedido,

        @NotEmpty(message = "El historial no puede estar vacío")
        @Size(max = 20, message = "El historial no puede tener más de 20 turnos")
        List<@Valid TurnoDto> historial) {
}

