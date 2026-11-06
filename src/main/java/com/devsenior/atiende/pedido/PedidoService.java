package com.devsenior.atiende.pedido;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM",
            Locale.forLanguageTag("es-CO"));

    private final Map<Long, Pedido> pedidos;

    public PedidoService() {
        LocalDate hoy = LocalDate.now();
        this.pedidos = Map.of(
                1045L, new Pedido(1045L, "Café de Huila 500 g", EstadoPedido.ENVIADO,
                        "TransAndina", "TA-778201", hoy.minusDays(3)),
                1046L, new Pedido(1046L, "Café de Nariño 250 g", EstadoPedido.ENTREGADO,
                        "Servientrega", "SE-882233", hoy.minusDays(1)),
                1050L, new Pedido(1050L, "Café de Tolima 500 g", EstadoPedido.EN_PREPARACION,
                        null, null, hoy.plusDays(4)));
    }

    public Optional<Pedido> buscar(Long numero) {
        return Optional.ofNullable(pedidos.get(numero));
    }

    public Optional<Pedido> buscar(String numero) {
        if (numero == null || numero.isBlank()) {
            return Optional.empty();
        }
        try {
            return buscar(Long.valueOf(numero.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public String datosVerificados(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        return buscar(numero)
                .map(this::describir)
                .orElse(null);
    }

    private String describir(Pedido pedido) {
        String producto = "- El pedido " + pedido.numero() + " (" + pedido.producto() + ")";
        String fecha = pedido.entregaEstimada() != null ? pedido.entregaEstimada().format(FORMATO_FECHA) : "";
        return switch (pedido.estado()) {
            case EN_PREPARACION -> producto + " está en preparación (EN_PREPARACION) y todavía no ha salido.\n"
                    + "- La entrega está estimada para el " + fecha + ".";
            case ENVIADO -> producto + " fue enviado con " + pedido.transportadora()
                    + ", guía " + pedido.numeroGuia() + ".\n" + describirEntrega(pedido, fecha);
            case ENTREGADO -> {
                String desc = producto + " ya fue entregado (ENTREGADO).";
                if (pedido.transportadora() != null) {
                    desc += " Transportadora: " + pedido.transportadora() + ".";
                }
                yield desc;
            }
        };
    }

    private String describirEntrega(Pedido pedido, String fecha) {
        long diasDeRetraso = ChronoUnit.DAYS.between(pedido.entregaEstimada(), LocalDate.now());
        if (diasDeRetraso > 0) {
            return "- La entrega estaba estimada para el " + fecha + ": lleva "
                    + diasDeRetraso + " días de retraso.";
        }
        return "- La entrega está estimada para el " + fecha + ".";
    }
}
