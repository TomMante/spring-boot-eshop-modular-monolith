package io.github.tommante.eshopmodularmonolith.shared.contract.error;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> fieldErrors
) {}
