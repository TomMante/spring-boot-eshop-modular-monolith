package io.github.tommante.eshopmodularmonolith.shared.infrastructure.error;

import io.github.tommante.eshopmodularmonolith.shared.contract.error.ApiError;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public class ApiErrors {

    private ApiErrors() {
    }


    public static ResponseEntity<ApiError> validation(Map<String, String> fields) {
        return ResponseEntity.badRequest()
                .body(new ApiError(
                                "validation.failed",
                                "Validation failed",
                                fields
                        )
                );
    }

    public static ResponseEntity<ApiError> badRequest(String code, String message){
        return ResponseEntity.badRequest()
                .body(new ApiError(code, message, null));
    }

    public static ResponseEntity<ApiError> conflict(String code, String message){
        return ResponseEntity.status(409)
                .body(new ApiError(code, message, null));
    }

    public static ResponseEntity<ApiError> notFound(String code, String message){
        return ResponseEntity.status(404)
                .body(new ApiError(code, message, null));
    }

    public static ResponseEntity<ApiError> paymentRequired(String code, String message){
        return ResponseEntity.status(402)
                .body(new ApiError(code, message, null));
    }

    public static ResponseEntity<ApiError> serviceUnavailable(String code, String message){
        return ResponseEntity.status(503)
                .body(new ApiError(code, message, null));
    }
}
