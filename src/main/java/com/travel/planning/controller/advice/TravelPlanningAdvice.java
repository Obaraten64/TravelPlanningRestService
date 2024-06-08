package com.travel.planning.controller.advice;

import com.travel.planning.exception.ServicesException;
import com.travel.planning.exception.TravelException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class TravelPlanningAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleArgumentNotValid(MethodArgumentNotValidException exception) {
        Optional<String> message = Optional.ofNullable(
                exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
        return Map.of("error", message.orElse(exception.getMessage()));
    }

    @ExceptionHandler(TravelException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleTravelException(TravelException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(ServicesException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleServicesException(ServicesException exception) {
        return Map.of("error", exception.getMessage());
    }
}
