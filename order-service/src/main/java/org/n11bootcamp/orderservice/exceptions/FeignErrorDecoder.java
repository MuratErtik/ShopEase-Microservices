package org.n11bootcamp.orderservice.exceptions;

import feign.Response;
import feign.codec.ErrorDecoder;


public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 404 -> new ResourceNotFoundException(
                    "Resource not found. method: " + methodKey);
            case 503 -> new ServiceUnavailableException(
                    "Service unavailable. method: " + methodKey);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}