package com.devsenior.atiende.ticket.dto;

import java.time.LocalDateTime;

import com.devsenior.atiende.ticket.CanalTicket;
import com.devsenior.atiende.ticket.Categoria;
import com.devsenior.atiende.ticket.EstadoTicket;
import com.devsenior.atiende.ticket.PrioridadTicket;
import com.devsenior.atiende.ticket.SentimientoTicket;

public record TicketRespuesta(
        Long id,
        String clienteNombre,
        String clienteContacto,
        CanalTicket canal,
        String mensaje,
        EstadoTicket estado,
        PrioridadTicket prioridad,
        Categoria categoria,
        SentimientoTicket sentimiento,
        String resumen,
        String numeroPedido,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion) {

    public TicketRespuesta(
            Long id,
            String clienteNombre,
            String clienteContacto,
            CanalTicket canal,
            String mensaje,
            EstadoTicket estado,
            PrioridadTicket prioridad,
            LocalDateTime fechaCreacion,
            LocalDateTime fechaActualizacion) {
        this(id, clienteNombre, clienteContacto, canal, mensaje, estado, prioridad, null, null, null, null, fechaCreacion, fechaActualizacion);
    }
}
