package com.devsenior.atiende.ticket;

public class TransicionEstadoNoPermitidaException extends RuntimeException {

    public TransicionEstadoNoPermitidaException(EstadoTicket origen, EstadoTicket destino) {
        super("No se permite cambiar el estado de " + origen + " a " + destino);
    }
}
