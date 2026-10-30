package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.web.api.docs.exception.RuleNotFoundException;
import ch.admin.bit.jeap.governance.web.api.docs.exception.SystemNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Answers an unknown system or rule of the docs API with {@code 404} as an RFC 9457 problem document.
 */
@RestControllerAdvice(assignableTypes = {SystemsController.class, RulesController.class})
class DocsApiExceptionHandler {

    @ExceptionHandler({SystemNotFoundException.class, RuleNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }
}
