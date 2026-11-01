package com.devsenior.atiende.ia;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class AsistenteIA {

    private static final Logger log = LoggerFactory.getLogger(AsistenteIA.class);

    private final List<ProveedorIA> proveedores;

    @Autowired
    public AsistenteIA(ProveedorOpenAi proveedorOpenAi, ProveedorClaude proveedorClaude) {
        this(List.of(proveedorOpenAi, proveedorClaude));
    }

    AsistenteIA(List<ProveedorIA> proveedores) {
        this.proveedores = proveedores;
    }

    public RespuestaIA completar(String instrucciones, String mensaje) {
        for (ProveedorIA proveedor : proveedores) {
            try {
                RespuestaIA respuesta = proveedor.completar(instrucciones, mensaje);
                if (respuesta != null && respuesta.texto() != null && !respuesta.texto().isBlank()) {
                    return respuesta;
                }
                log.warn("El proveedor {} devolvió una respuesta vacía", proveedor.nombre());
            } catch (RestClientResponseException ex) {
                int codigo = ex.getStatusCode().value();
                if (codigo == 400 || codigo == 401 || codigo == 403 || codigo == 404) {
                    log.error("Error de configuración al llamar al proveedor {}: código HTTP {}", proveedor.nombre(), codigo);
                } else {
                    log.warn("Error temporal al llamar al proveedor {}: código HTTP {}", proveedor.nombre(), codigo);
                }
            } catch (ResourceAccessException ex) {
                log.warn("Error temporal de red o tiempo de espera al llamar al proveedor {}: {}", proveedor.nombre(), ex.getMessage());
            } catch (Exception ex) {
                log.warn("Error temporal al llamar al proveedor {}: {}", proveedor.nombre(), ex.getMessage());
            }
        }

        throw new IaNoDisponibleException("Todos los proveedores de IA fallaron al procesar la solicitud");
    }
}

