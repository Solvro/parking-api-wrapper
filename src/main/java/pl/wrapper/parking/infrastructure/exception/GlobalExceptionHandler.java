package pl.wrapper.parking.infrastructure.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import pl.wrapper.parking.infrastructure.error.ErrorWrapper;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j
class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorWrapper> handleGeneralException(Exception e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "An error has occurred";
        ErrorWrapper errorWrapper = new ErrorWrapper(message, status, request.getRequestURI(), status);
        logError(message, request.getRequestURI(), e);
        return new ResponseEntity<>(errorWrapper, status);
    }

    @ExceptionHandler(NominatimClientException.class)
    public ResponseEntity<ErrorWrapper> handleNominatimClientException(
            NominatimClientException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String message = ex.getMessage();
        ErrorWrapper errorWrapper = new ErrorWrapper(message, status, request.getRequestURI(), status);
        logError(message, request.getRequestURI(), ex);
        return new ResponseEntity<>(errorWrapper, status);
    }

    @ExceptionHandler(InvalidCallException.class)
    public ResponseEntity<ErrorWrapper> handleInvalidCallException(InvalidCallException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Invalid call";
        ErrorWrapper errorWrapper = new ErrorWrapper(message, status, request.getRequestURI(), status);
        logError(message, request.getRequestURI(), e);
        return new ResponseEntity<>(errorWrapper, status);
    }

    @ExceptionHandler(PwrApiNotRespondingException.class)
    public ResponseEntity<ErrorWrapper> handlePwrApiNotRespondingException(
            PwrApiNotRespondingException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String message = "PWR Api not responding";
        ErrorWrapper errorWrapper = new ErrorWrapper(message, status, request.getRequestURI(), status);
        logError(message, request.getRequestURI(), e);
        return new ResponseEntity<>(errorWrapper, status);
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<ErrorWrapper> handleJsonProcessingException(
            JsonProcessingException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Json processing error";
        ErrorWrapper errorWrapper = new ErrorWrapper(message, status, request.getRequestURI(), status);
        logError(message, request.getRequestURI(), e);
        return new ResponseEntity<>(errorWrapper, status);
    }

    @ExceptionHandler(ClassCastException.class)
    public ResponseEntity<ErrorWrapper> handleClassCastException(ClassCastException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Class cast exception";
        ErrorWrapper errorWrapper = new ErrorWrapper(message, status, request.getRequestURI(), status);
        logError(message, request.getRequestURI(), e);
        return new ResponseEntity<>(errorWrapper, status);
    }

    @ExceptionHandler(SerializationFailedException.class)
    public ResponseEntity<ErrorWrapper> handleClassCastException(
            SerializationFailedException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorWrapper errorWrapper = new ErrorWrapper(e.getMessage(), status, request.getRequestURI(), status);
        logError(e.getMessage(), request.getRequestURI(), e);
        return new ResponseEntity<>(errorWrapper, status);
    }

    private <T extends Exception> void logError(String message, String uri, T e) {
        log.error("{} at uri: {}; Details: {}", message, uri, e.getMessage());
    }
}
