package ru.itmo.marines.error;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppValidationException.class)
    public ResponseEntity<Map<String, Object>> validation(AppValidationException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), e.getProblems());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(NotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflict(ConflictException e) {
        return body(HttpStatus.CONFLICT, e.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST,
                "Не удалось разобрать данные запроса: проверьте формат чисел и полей", List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> typeMismatch(MethodArgumentTypeMismatchException e) {
        return body(HttpStatus.BAD_REQUEST, "Неверное значение параметра «" + e.getName() + "»", List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> notValid(MethodArgumentNotValidException e) {
        List<FieldProblem> problems = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new FieldProblem(f.getField(), f.getDefaultMessage()))
                .toList();
        return body(HttpStatus.BAD_REQUEST, "Некорректные данные", problems);
    }

    @ExceptionHandler({ConstraintViolationException.class, TransactionSystemException.class})
    public ResponseEntity<Map<String, Object>> constraint(Exception e) {
        log.warn("Нарушено ограничение при сохранении", e);
        return body(HttpStatus.BAD_REQUEST, "Данные не прошли проверку ограничений на уровне хранилища", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integrity(DataIntegrityViolationException e) {
        log.warn("Нарушение целостности БД", e);
        return body(HttpStatus.CONFLICT, "Операция нарушает ограничения целостности базы данных", List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> illegalState(IllegalStateException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> other(Exception e) {
        if (e instanceof ErrorResponse er) { // стандартные ошибки Spring MVC (404, 405 и т.п.)
            return body(HttpStatus.valueOf(er.getStatusCode().value()), "Запрос не может быть выполнен", List.of());
        }
        log.error("Необработанная ошибка", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", List.of());
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, List<FieldProblem> errors) {
        return ResponseEntity.status(status).body(Map.of("message", message, "errors", errors));
    }
}
