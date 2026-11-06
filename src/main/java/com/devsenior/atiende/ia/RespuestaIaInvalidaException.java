package com.devsenior.atiende.ia;

public class RespuestaIaInvalidaException extends RuntimeException {

    public RespuestaIaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public RespuestaIaInvalidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
