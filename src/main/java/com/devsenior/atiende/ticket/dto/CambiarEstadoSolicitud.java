package com.devsenior.atiende.ticket.dto;

import com.devsenior.atiende.ticket.EstadoTicket;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoSolicitud(
        @NotNull EstadoTicket estado) {
}
