package com.devsenior.atiende.ticket;

import java.util.Set;

public enum EstadoTicket {
    ABIERTO,
    EN_PROCESO,
    RESUELTO,
    CERRADO;

    public boolean puedeCambiarA(EstadoTicket destino) {
        if (destino == null) {
            return false;
        }
        return transicionesPermitidas().contains(destino);
    }

    private Set<EstadoTicket> transicionesPermitidas() {
        return switch (this) {
            case ABIERTO -> Set.of(EN_PROCESO, CERRADO);
            case EN_PROCESO -> Set.of(RESUELTO);
            case RESUELTO -> Set.of(CERRADO, EN_PROCESO);
            case CERRADO -> Set.of();
        };
    }
}
