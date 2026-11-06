package com.devsenior.atiende.ia.dto;

import java.math.BigDecimal;

import com.devsenior.atiende.ia.RespuestaIA;

public record FacturaIA(
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimado,
        long milisegundos) {

    public static FacturaIA de(RespuestaIA respuesta) {
        if (respuesta == null) {
            return null;
        }
        return new FacturaIA(
                respuesta.proveedor(),
                respuesta.modelo(),
                respuesta.tokensEntrada(),
                respuesta.tokensSalida(),
                respuesta.costoEstimado(),
                respuesta.milisegundos());
    }
}

