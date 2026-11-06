package com.devsenior.atiende.ticket;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devsenior.atiende.compartido.RecursoNoEncontradoException;
import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ticket.dto.CambiarEstadoSolicitud;
import com.devsenior.atiende.ticket.dto.CrearTicketSolicitud;
import com.devsenior.atiende.ticket.dto.SentimientoRespuesta;
import com.devsenior.atiende.ticket.dto.TicketRespuesta;

@Service
public class TicketServicio {

    private final TicketRepositorio ticketRepositorio;
    private final TicketMapper ticketMapper;
    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;

    @Autowired
    public TicketServicio(
            TicketRepositorio ticketRepositorio,
            TicketMapper ticketMapper,
            AsistenteIA asistenteIA,
            BibliotecaPrompts bibliotecaPrompts) {
        this.ticketRepositorio = ticketRepositorio;
        this.ticketMapper = ticketMapper;
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
    }

    public TicketServicio(TicketRepositorio ticketRepositorio, TicketMapper ticketMapper) {
        this(ticketRepositorio, ticketMapper, null, null);
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

    @Transactional(readOnly = true)
    public SentimientoRespuesta analizarSentimiento(Long id) {
        Ticket ticket = buscarOFallar(id);
        String instrucciones = bibliotecaPrompts.cargar("sentimiento");
        RespuestaIA respuesta = asistenteIA.completar(instrucciones, ticket.getMensaje());

        SentimientoTicket sentimiento = SentimientoTicket.desdeTexto(respuesta.texto());
        if (sentimiento == null) {
            throw new RespuestaIaInvalidaException("La IA devolvió un sentimiento no reconocido: " + respuesta.texto());
        }

        return new SentimientoRespuesta(
                sentimiento,
                respuesta.proveedor(),
                respuesta.modelo(),
                respuesta.tokensEntrada(),
                respuesta.tokensSalida(),
                respuesta.costoEstimado(),
                respuesta.milisegundos());
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

    @Transactional
    public TicketRespuesta aplicarClasificacion(Long id, ClasificacionIA clasificacion, String numeroPedido) {
        Ticket ticket = buscarOFallar(id);
        ticket.setCategoria(clasificacion.categoria());
        ticket.setPrioridad(clasificacion.prioridad());
        ticket.setSentimiento(clasificacion.sentimiento());
        ticket.setResumen(clasificacion.resumen());
        ticket.setNumeroPedido(numeroPedido);
        ticket.setFechaActualizacion(LocalDateTime.now());
        Ticket guardado = ticketRepositorio.saveAndFlush(ticket);
        return ticketMapper.aRespuesta(guardado);
    }

    public Ticket buscarOFallar(Long id) {
        return ticketRepositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el ticket con id " + id));
    }
}

