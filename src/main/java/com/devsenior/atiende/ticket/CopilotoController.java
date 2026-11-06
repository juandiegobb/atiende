package com.devsenior.atiende.ticket;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devsenior.atiende.ticket.dto.BorradorResponse;
import com.devsenior.atiende.ticket.dto.ClasificacionResponse;

@RestController
@RequestMapping("/api/tickets")
public class CopilotoController {

    private final ClasificacionService clasificacionService;
    private final BorradorService borradorService;

    public CopilotoController(ClasificacionService clasificacionService, BorradorService borradorService) {
        this.clasificacionService = clasificacionService;
        this.borradorService = borradorService;
    }

    @PostMapping("/{id}/clasificacion")
    public ResponseEntity<ClasificacionResponse> clasificar(@PathVariable Long id) {
        return ResponseEntity.ok(clasificacionService.clasificar(id));
    }

    @PostMapping("/{id}/borrador")
    public ResponseEntity<BorradorResponse> generarBorrador(@PathVariable Long id) {
        return ResponseEntity.ok(borradorService.generarBorrador(id));
    }
}

