package com.youthjob.api.chat.support;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ChatExceptionAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e){
        String msg = e.getBindingResult().getAllErrors().stream()
                .findFirst().map(err -> err.getDefaultMessage()).orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraint(ConstraintViolationException e){
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleEtc(Exception e){
        return ResponseEntity.internalServerError().body(Map.of("error", "서버 오류가 발생했습니다."));
    }
}
