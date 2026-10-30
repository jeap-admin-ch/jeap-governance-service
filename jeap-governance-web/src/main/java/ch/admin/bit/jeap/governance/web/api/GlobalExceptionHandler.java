package ch.admin.bit.jeap.governance.web.api;

import ch.admin.bit.jeap.governance.web.api.docs.exception.RuleNotFoundException;
import ch.admin.bit.jeap.governance.web.api.docs.exception.SystemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(SystemNotFoundException.class)
    public ResponseEntity<String> handleSystemNotFoundException(SystemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(RuleNotFoundException.class)
    public ResponseEntity<String> handleRuleNotFoundException(RuleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
