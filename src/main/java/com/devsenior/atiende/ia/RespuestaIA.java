package com.devsenior.atiende.ia;

import java.math.BigDecimal;

public record RespuestaIA(
        String texto,
        String proveedor,
        String modelo,
        int tokensEntrada,
        int tokensSalida,
        BigDecimal costoEstimado,
        long milisegundos) {
}

