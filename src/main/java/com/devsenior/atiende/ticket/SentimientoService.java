package com.devsenior.atiende.ticket;

import org.springframework.stereotype.Service;

import com.devsenior.atiende.ia.AsistenteIA;
import com.devsenior.atiende.ia.BibliotecaPrompts;
import com.devsenior.atiende.ia.RespuestaIA;
import com.devsenior.atiende.ia.RespuestaIaInvalidaException;
import com.devsenior.atiende.ticket.dto.SentimientoRespuesta;

@Service
public class SentimientoService {

    private final AsistenteIA asistenteIA;
    private final BibliotecaPrompts bibliotecaPrompts;

    public SentimientoService(AsistenteIA asistenteIA, BibliotecaPrompts bibliotecaPrompts) {
        this.asistenteIA = asistenteIA;
        this.bibliotecaPrompts = bibliotecaPrompts;
    }

    public SentimientoRespuesta analizar(String mensaje) {
        String instrucciones = bibliotecaPrompts.cargar("sentimiento");
        RespuestaIA respuesta = asistenteIA.completar(instrucciones, mensaje);

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
}
