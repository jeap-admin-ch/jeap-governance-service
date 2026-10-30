package ch.admin.bit.jeap.governance.web.api.docs;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Entity tags and conditional requests for the docs API.
 * <p>
 * A tag is always {@code "sha256:<hex>"} over the serialized body, so the tag an index lists for a resource is
 * byte-identical to the tag that resource answers with, as long as both are computed from the same body - which is
 * why the indexes build every resource they list with the same code as the resource itself.
 */
@Component
@RequiredArgsConstructor
class DocsApiEtagSupport {

    private final ObjectMapper objectMapper;

    byte[] serialize(Object body) {
        return objectMapper.writeValueAsBytes(body);
    }

    String entityTagOf(Object body) {
        return entityTagOfBytes(serialize(body));
    }

    private static String entityTagOfBytes(byte[] serializedBody) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(serializedBody);
            return "\"sha256:" + HexFormat.of().formatHex(hash) + "\"";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Answers with the body and its entity tag, or with {@code 304} and an empty body when the caller already has it.
     *
     * @return the response, or null when the prepared {@code 304} is to be sent
     */
    ResponseEntity<byte[]> respond(WebRequest request, Object body) {
        byte[] serialized = serialize(body);
        String entityTag = entityTagOfBytes(serialized);
        if (request.checkNotModified(entityTag)) {
            // Spring writes the 304 itself, bypassing the ResponseEntity, so the directive is repeated here
            setNoCache(request);
            return null;
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .eTag(entityTag)
                .cacheControl(CacheControl.noCache())
                .body(serialized);
    }

    private static void setNoCache(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletResponse response = servletWebRequest.getResponse();
            if (response != null) {
                response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue());
            }
        }
    }
}
