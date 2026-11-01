package com.devsenior.atiende.ia;

public interface ProveedorIA {

    String nombre();

    RespuestaIA completar(String instrucciones, String mensaje);
}

