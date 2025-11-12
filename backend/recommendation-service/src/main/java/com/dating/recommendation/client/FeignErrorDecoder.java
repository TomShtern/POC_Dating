package com.dating.recommendation.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Custom error decoder for Feign client errors.
 * Converts Feign errors into appropriate Spring exceptions.
 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());

        if (status == null) {
            return defaultDecoder.decode(methodKey, response);
        }

        String errorMessage = String.format("Error calling %s: %s %s",
                methodKey, response.status(), response.reason());

        log.error(errorMessage);

        return switch (status) {
            case NOT_FOUND -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Resource not found in downstream service"
            );
            case UNAUTHORIZED, FORBIDDEN -> new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication failed in downstream service"
            );
            case BAD_REQUEST -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid request to downstream service"
            );
            case SERVICE_UNAVAILABLE -> new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Downstream service unavailable"
            );
            default -> new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error communicating with downstream service"
            );
        };
    }
}
