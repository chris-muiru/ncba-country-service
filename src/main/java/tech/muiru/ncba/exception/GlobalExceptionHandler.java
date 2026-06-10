package tech.muiru.ncba.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomExceptionDto handleCustomException(CustomException ex, HttpServletRequest request) {
        log.info("CustomException - path: {}, message: {}", request.getRequestURI(), ex.getMessage());
        return new CustomExceptionDto(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomExceptionDto handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.info("MissingBody - path: {}", request.getRequestURI());
        return new CustomExceptionDto(HttpStatus.BAD_REQUEST.value(), "Request body is missing or malformed",
                HttpStatus.BAD_REQUEST.getReasonPhrase(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomExceptionDto handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.info("ValidationException - path: {}, errors: {}", request.getRequestURI(), errors);
        return new CustomExceptionDto(
                HttpStatus.BAD_REQUEST.value(),
                errors,
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CustomExceptionDto handleGenericException(Exception ex, HttpServletRequest request) {
        log.info("UnhandledException - path: {}, message: {}", request.getRequestURI(), ex.getMessage(), ex);
        return new CustomExceptionDto(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                request.getRequestURI()
        );
    }
}
