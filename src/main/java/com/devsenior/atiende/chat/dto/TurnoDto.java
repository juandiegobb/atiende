package com.devsenior.atiende.chat.dto;

import com.devsenior.atiende.chat.RolTurno;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TurnoDto(
        @NotNull(message = "El rol es obligatorio")
        RolTurno rol,

        @NotBlank(message = "El texto del turno no puede estar vacío")
        @Size(max = 500, message = "El texto del turno no puede superar 500 caracteres")
        String texto) {
}

