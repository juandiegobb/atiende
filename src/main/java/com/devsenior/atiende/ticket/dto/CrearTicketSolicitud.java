package com.devsenior.atiende.ticket.dto;

import com.devsenior.atiende.ticket.CanalTicket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearTicketSolicitud(
        @NotBlank @Size(max = 100) String clienteNombre,
        @NotBlank @Size(max = 120) String clienteContacto,
        @NotNull CanalTicket canal,
        @NotBlank @Size(max = 2000) String mensaje) {
}
