package io.github.tommante.eshopmodularmonolith.e2e.api;

import net.serenitybdd.screenplay.rest.questions.RestQueryFunction;

import java.util.List;

public final class Api {

    private Api() {
    }

    public static Send get(String path, RestQueryFunction... customisations) {
        return new Send(Send.Method.GET, path, List.of(customisations), false);
    }

    public static Send post(String path, RestQueryFunction... customisations) {
        return new Send(Send.Method.POST, path, List.of(customisations), false);
    }
}
