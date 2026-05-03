package org.n11bootcamp.paymentservice.exceptions;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldErrorResponse> fields = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        error.getDefaultMessage()))
                .toList();

        ValidationErrorResponse response = new ValidationErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                request.getRequestURI(),
                fields
        );

        return ResponseEntity.badRequest().body(response);
    }



    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "Invalid request body format for enum types.";
        log.error("Message not readable: {}", ex.getMessage());

        if (ex.getCause() instanceof InvalidFormatException formatException) {
            String fieldName = formatException.getPath().isEmpty() ? "field" : formatException.getPath().get(0).getFieldName();
            String invalidValue = formatException.getValue().toString();

            if (formatException.getTargetType().isEnum()) {
                String allowedValues = Arrays.toString(formatException.getTargetType().getEnumConstants());
                message = String.format("Invalid value '%s' for field '%s'. Accepted values are: %s",
                        invalidValue, fieldName, allowedValues);
            } else {
                message = String.format("Invalid data type for field '%s': '%s'. Expected type: %s",
                        fieldName, invalidValue, formatException.getTargetType().getSimpleName());
            }
        }

        ErrorDetail error = ErrorDetail.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String name = ex.getName();
        String type = ex.getRequiredType().getSimpleName();
        Object value = ex.getValue();

        String message = String.format("The value '%s' is invalid for parameter '%s'. Expected type: %s", value, name, type);


        return ErrorDetail.builder()
                .error(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(InsufficientStockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorDetail handleSellerNotAuthorizedThisProductException(InsufficientStockException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDetail handleProductNotFoundException(ProductNotFoundException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }


    @ExceptionHandler(PaymentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDetail handlePaymentNotFoundException(PaymentNotFoundException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDetail handleInventoryAlreadyExistsException(ResourceNotFoundException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ResponseBody
    public ErrorDetail handleServiceUnavailableException(ServiceUnavailableException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .timestamp(LocalDateTime.now())
                .build();
    }


    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public ErrorDetail handleUnauthorizedException(UnauthorizedException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.UNAUTHORIZED.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDetail handleCartItemNotFoundException(CartItemNotFoundException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }



    @ExceptionHandler(EmptyCartException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorDetail handleEmptyCartException(EmptyCartException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.CONFLICT.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDetail handleOrderNotFoundException(OrderNotFoundException ex, HttpServletRequest request) {
        return ErrorDetail.builder()
                .error(ex.getMessage())
                .status(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .build();
    }












}
