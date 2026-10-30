package ch.admin.bit.jeap.governance.web.api.docs;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/**
 * The routes of the docs API, kept in one place so that the controllers, the security configuration and the links
 * the indexes carry derive from the same constants.
 */
public final class DocsApiPaths {

    public static final String DOCS_API = "/docs-api";
    public static final String SYSTEMS = DOCS_API + "/systems";
    public static final String RULES = DOCS_API + "/rules";

    private DocsApiPaths() {
    }

    /**
     * @return the path of a system resource, with the service's context path and without host
     */
    public static String systemPath(String systemName) {
        return contextPath() + SYSTEMS + "/" + UriUtils.encodePathSegment(systemName, StandardCharsets.UTF_8);
    }

    /**
     * @return the path of a rule resource, with the service's context path and without host
     */
    public static String rulePath(String ruleId) {
        return contextPath() + RULES + "/" + UriUtils.encodePathSegment(ruleId, StandardCharsets.UTF_8);
    }

    /**
     * Instances run under {@code server.servlet.context-path}, so a link without it would not resolve against the
     * base URL a consumer called. Empty outside a request.
     */
    private static String contextPath() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest().getContextPath();
        }
        return "";
    }
}
