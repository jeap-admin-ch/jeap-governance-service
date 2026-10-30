package ch.admin.bit.jeap.governance.web.api.docs;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every handler method of the docs API requires its role; one added without it fails here.
 */
class DocsApiRoleCoverageTest {

    @Test
    void everyDocsApiHandlerMethodRequiresTheRole() {
        List<String> unprotected = new ArrayList<>();
        for (Class<?> controller : docsApiControllers()) {
            for (Method method : controller.getDeclaredMethods()) {
                if (isHandlerMethod(method) && !method.isAnnotationPresent(RequiresGovernanceDocumentationRead.class)) {
                    unprotected.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }
        assertThat(unprotected)
                .withFailMessage("Every docs API handler method must be annotated with "
                                 + "@RequiresGovernanceDocumentationRead, these are not: %s", unprotected)
                .isEmpty();
    }

    @Test
    void theControllersAreFound() {
        assertThat(docsApiControllers()).extracting(Class::getSimpleName)
                .containsExactlyInAnyOrder("SystemsController", "RulesController");
    }

    @Test
    void theRoleAnnotationCarriesTheSemanticRole() {
        assertThat(RequiresGovernanceDocumentationRead.class.getAnnotation(PreAuthorize.class).value())
                .isEqualTo("hasRole('governance-documentation', 'read')");
    }

    private static boolean isHandlerMethod(Method method) {
        return method.isAnnotationPresent(RequestMapping.class) || Arrays.stream(method.getAnnotations())
                .anyMatch(annotation -> annotation.annotationType().isAnnotationPresent(RequestMapping.class));
    }

    private static List<Class<?>> docsApiControllers() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false, new StandardEnvironment());
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        return scanner.findCandidateComponents(DocsApiRoleCoverageTest.class.getPackageName()).stream()
                .<Class<?>>map(definition -> {
                    try {
                        return Class.forName(definition.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .toList();
    }
}
