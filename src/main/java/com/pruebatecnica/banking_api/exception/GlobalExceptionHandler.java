package com.pruebatecnica.banking_api.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        log.warn("Error de negocio: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody(ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    // Captura errores de @Valid en @RequestBody (ej: monto null, monto <= 0)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(errorBody(mensaje));
    }

    // Captura errores de @Validated en @RequestParam (ej: currency vacío, amount <= 0)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String mensaje = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath().toString().replaceAll(".*\\.", "") + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(errorBody(mensaje));
    }

    // Captura conflictos de concurrencia (dos transacciones modificando la misma cuenta simultáneamente)
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(OptimisticLockException ex) {
        log.warn("Conflicto de concurrencia detectado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(errorBody("La operación no se pudo completar por un conflicto de concurrencia. Intente nuevamente."));
    }

    // Captura path variables con tipo incorrecto (ej: /clientes/abc/resumen)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String mensaje = String.format("El parámetro '%s' debe ser de tipo %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido");
        return ResponseEntity.badRequest().body(errorBody(mensaje));
    }

    // Captura TODAS las excepciones no manejadas por handlers específicos (red de seguridad)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Loggea el error completo con stack trace para debugging
        String errorId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.error("Error inesperado [ID: {}]: {}", errorId, ex.getMessage(), ex);
        
        // Mensaje genérico al cliente (no expone detalles internos por seguridad)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Ha ocurrido un error inesperado. Por favor contacte soporte con el código: " + errorId));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of(
                "error", message,
                "timestamp", LocalDateTime.now().toString()
        );
    }
}
