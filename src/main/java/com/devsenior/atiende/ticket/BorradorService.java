package com.devsenior.atiende.ticket;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.LectorJsonIA;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ia.dto.FacturaIA;
import com.devsenior.atiende.pedido.PedidoService;
import com.devsenior.atiende.ticket.dto.BorradorResponse;

@Service
public class BorradorService {

    private final TicketServicio ticketServicio;
    private final PedidoService pedidoService;
    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;
    private final LectorJsonIA lectorJsonIA;

    public BorradorService(
            TicketServicio ticketServicio,
            PedidoService pedidoService,
            AsistenteIA asistenteIA,
            BibliotecaPrompts bibliotecaPrompts,
            LectorJsonIA lectorJsonIA) {
        this.ticketServicio = ticketServicio;
        this.pedidoService = pedidoService;
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
        this.lectorJsonIA = lectorJsonIA;
    }

    public BorradorResponse generarBorrador(Long id) {
        Ticket ticket = ticketServicio.buscarOFallar(id);

        String datosVerificados = ticket.getNumeroPedido() != null
                ? pedidoService.datosVerificados(ticket.getNumeroPedido())
                : null;
        String datos = datosVerificados != null ? datosVerificados : "No hay datos verificados para este pedido.";

        String formato = ticket.getCanal() == CanalTicket.CORREO
                ? "correo electrónico con asunto y cuerpo"
                : "mensaje directo o chat sin asunto (solo cuerpo)";

        Map<String, String> variables = Map.of(
                "cliente", ticket.getClienteNombre(),
                "formato", formato,
                "datos", datos);

        String instrucciones = bibliotecaPrompts.cargar("borrador-respuesta", variables);
        RespuestaIA respuestaIA = asistenteIA.completar(instrucciones, ticket.getMensaje());

        BorradorIA borrador = lectorJsonIA.leer(respuestaIA, BorradorIA.class);
        if (borrador == null || !borrador.estaCompleta()) {
            throw new RespuestaIaInvalidaException("El borrador generado por la IA no contiene cuerpo válido");
        }

        String asunto = ticket.getCanal() == CanalTicket.CORREO ? borrador.asunto() : null;
        FacturaIA factura = FacturaIA.de(respuestaIA);

        return new BorradorResponse(ticket.getId(), ticket.getCanal(), asunto, borrador.cuerpo(), factura);
    }
}
