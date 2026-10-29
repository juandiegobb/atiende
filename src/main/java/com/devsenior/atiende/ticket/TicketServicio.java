package com.devsenior.atiende.ticket;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.atiende.compartido.RecursoNoEncontradoException;
import com.devsenior.atiende.ticket.dto.CambiarEstadoSolicitud;
import com.devsenior.atiende.ticket.dto.CrearTicketSolicitud;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@Service
public class TicketServicio {

    private final TicketRepositorio ticketRepositorio;
    private final TicketMapper ticketMapper;

    public TicketServicio(TicketRepositorio ticketRepositorio, TicketMapper ticketMapper) {
        this.ticketRepositorio = ticketRepositorio;
        this.ticketMapper = ticketMapper;
    }

    @Transactional
    public TicketRespuesta crear(CrearTicketSolicitud solicitud) {
        Ticket ticket = new Ticket();
        ticket.setClienteNombre(solicitud.clienteNombre());
        ticket.setClienteContacto(solicitud.clienteContacto());
        ticket.setCanal(solicitud.canal());
        ticket.setMensaje(solicitud.mensaje());
        ticket.setEstado(EstadoTicket.ABIERTO);
        ticket.setPrioridad(PrioridadTicket.MEDIA);
        Ticket guardado = ticketRepositorio.save(ticket);
        return ticketMapper.aRespuesta(guardado);
    }

    @Transactional(readOnly = true)
    public List<TicketRespuesta> listar(EstadoTicket estado) {
        List<Ticket> tickets = estado == null
                ? ticketRepositorio.findAllByOrderByFechaCreacionDesc()
                : ticketRepositorio.findByEstadoOrderByFechaCreacionDesc(estado);
        return tickets.stream().map(ticketMapper::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public TicketRespuesta obtenerPorId(Long id) {
        Ticket ticket = buscarOFallar(id);
        return ticketMapper.aRespuesta(ticket);
    }

    @Transactional
    public TicketRespuesta cambiarEstado(Long id, CambiarEstadoSolicitud solicitud) {
        Ticket ticket = buscarOFallar(id);
        EstadoTicket destino = solicitud.estado();
        if (!ticket.getEstado().puedeCambiarA(destino)) {
            throw new TransicionEstadoNoPermitidaException(ticket.getEstado(), destino);
        }
        ticket.setEstado(destino);
        ticket.setFechaActualizacion(LocalDateTime.now());
        Ticket actualizado = ticketRepositorio.saveAndFlush(ticket);
        return ticketMapper.aRespuesta(actualizado);
    }

    private Ticket buscarOFallar(Long id) {
        return ticketRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ticket con id " + id));
    }
}
