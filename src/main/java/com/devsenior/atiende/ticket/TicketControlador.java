package com.devsenior.atiende.ticket;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.atiende.ticket.dto.CambiarEstadoSolicitud;
import com.devsenior.atiende.ticket.dto.CrearTicketSolicitud;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tickets")
public class TicketControlador {

    private final TicketServicio ticketServicio;

    public TicketControlador(TicketServicio ticketServicio) {
        this.ticketServicio = ticketServicio;
    }

    @PostMapping
    public ResponseEntity<TicketRespuesta> crear(@Valid @RequestBody CrearTicketSolicitud solicitud) {
        TicketRespuesta creado = ticketServicio.crear(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping
    public ResponseEntity<List<TicketRespuesta>> listar(@RequestParam(required = false) EstadoTicket estado) {
        return ResponseEntity.ok(ticketServicio.listar(estado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketRespuesta> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ticketServicio.obtenerPorId(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TicketRespuesta> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoSolicitud solicitud) {
        return ResponseEntity.ok(ticketServicio.cambiarEstado(id, solicitud));
    }
}
