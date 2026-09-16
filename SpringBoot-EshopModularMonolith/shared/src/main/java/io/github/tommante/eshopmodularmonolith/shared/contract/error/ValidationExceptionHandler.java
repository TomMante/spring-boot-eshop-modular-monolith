package io.github.tommante.eshopmodularmonolith.shared.contract.error;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handle(MethodArgumentNotValidException exception){
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        DefaultMessageSourceResolvable::getDefaultMessage
                ));
        return ApiErrors.validation(fieldErrors);
    }
}
