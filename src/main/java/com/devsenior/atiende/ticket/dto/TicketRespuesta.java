package com.devsenior.atiende.ticket.dto;

import java.time.LocalDateTime;

import com.devsenior.atiende.ticket.CanalTicket;
import com.devsenior.atiende.ticket.EstadoTicket;
import com.devsenior.atiende.ticket.PrioridadTicket;

public record TicketRespuesta(
        Long id,
        String clienteNombre,
        String clienteContacto,
        CanalTicket canal,
        String mensaje,
        EstadoTicket estado,
        PrioridadTicket prioridad,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {
}
