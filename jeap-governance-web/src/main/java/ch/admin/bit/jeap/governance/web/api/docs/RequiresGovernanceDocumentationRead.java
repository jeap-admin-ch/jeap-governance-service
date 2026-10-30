package ch.admin.bit.jeap.governance.web.api.docs;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires the semantic role {@code <system-name>_@governance-documentation_#read}. Every resource of the docs API
 * carries it; {@code DocsApiRoleCoverageTest} fails the build if a handler method is added without it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@PreAuthorize("hasRole('governance-documentation', 'read')")
public @interface RequiresGovernanceDocumentationRead {
}
