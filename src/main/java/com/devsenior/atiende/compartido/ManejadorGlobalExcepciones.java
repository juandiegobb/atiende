package com.devsenior.atiende.compartido;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.devsenior.atiende.ticket.TransicionEstadoNoPermitidaException;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        String campo = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "desconocido"
                : ex.getBindingResult().getFieldErrors().get(0).getField();
        String detalle = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Datos inválidos"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                HttpStatus.BAD_REQUEST.value(),
                "DATOS_INVALIDOS",
                "El campo " + campo + " es inválido: " + detalle,
                LocalDateTime.now());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ErrorRespuesta> manejarPeticionMalformada(Exception ex) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                HttpStatus.BAD_REQUEST.value(),
                "PETICION_MALFORMADA",
                "La petición no se pudo interpretar. Revisa el formato y los valores enviados.",
                LocalDateTime.now());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                HttpStatus.NOT_FOUND.value(),
                "NO_ENCONTRADO",
                ex.getMessage(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
    }

    @ExceptionHandler(TransicionEstadoNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarTransicion(TransicionEstadoNoPermitidaException ex) {
        ErrorRespuesta cuerpo = new ErrorRespuesta(
                HttpStatus.CONFLICT.value(),
                "TRANSICION_NO_PERMITIDA",
                ex.getMessage(),
                LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(cuerpo);
    }
}
