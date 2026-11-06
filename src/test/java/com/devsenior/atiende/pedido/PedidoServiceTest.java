package com.devsenior.atiende.pedido;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PedidoServiceTest {

    private PedidoService pedidoService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService();
    }

    @Test
    void datosVerificados_pedido1045Laura_incluyeTransportadoraGuiaY3DiasRetraso() {
        String datos = pedidoService.datosVerificados("1045");

        assertNotNull(datos);
        assertTrue(datos.contains("TransAndina"));
        assertTrue(datos.contains("TA-778201"));
        assertTrue(datos.contains("3 días de retraso"));
    }

    @Test
    void datosVerificados_pedido1046_retornaDatosSinRetraso() {
        String datos = pedidoService.datosVerificados("1046");

        assertNotNull(datos);
        assertTrue(datos.contains("1046"));
        assertTrue(datos.contains("ENTREGADO"));
        assertTrue(datos.contains("Servientrega"));
    }

    @Test
    void datosVerificados_pedido1050_retornaEnPreparacion() {
        String datos = pedidoService.datosVerificados("1050");

        assertNotNull(datos);
        assertTrue(datos.contains("1050"));
        assertTrue(datos.contains("EN_PREPARACION"));
    }

    @Test
    void datosVerificados_pedidoInexistente_retornaNull() {
        assertNull(pedidoService.datosVerificados("9999"));
        assertNull(pedidoService.datosVerificados(null));
    }
}

