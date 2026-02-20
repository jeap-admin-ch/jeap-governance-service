package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.reporting.ReportingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceClient;
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluencePage;
import org.sahli.asciidoc.confluence.publisher.client.http.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Component
@RequiredArgsConstructor
@Slf4j
class ConfluenceAdapterImpl implements ConfluenceAdapter {
    static final String CONTENT_HASH_PROPERTY_KEY = "content-hash";
    private static final String VERSION_MESSAGE = "Documentation generated";

    private final ConfluenceClient confluenceClient;
    private final ReportingProperties properties;

    @Override
    public String getPageByName(String pageName) {
        return confluenceClient.getPageByTitle(properties.getConfluenceSpaceKey(), properties.getConfluenceAncestorId(), pageName);
    }

    @Override
    public String addOrUpdatePageUnderAncestor(String ancestorId, String pageName, String content) {
        String contentId;
        try {
            contentId = this.confluenceClient.getPageByTitle(properties.getConfluenceSpaceKey(), ancestorId, pageName);
            updatePage(contentId, ancestorId, pageName, content);
        } catch (NotFoundException e) {
            log.info("Creating page {}", pageName);
            contentId = this.confluenceClient.addPageUnderAncestor(properties.getConfluenceSpaceKey(), ancestorId, pageName, content, VERSION_MESSAGE);
            this.confluenceClient.setPropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY, hash(content));
        }

        return contentId;
    }

    private void updatePage(String contentId, String ancestorId, String pageName, String content) {
        ConfluencePage existingPage = this.confluenceClient.getPageWithContentAndVersionById(contentId);
        String existingContentHash = this.confluenceClient.getPropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY);
        String newContentHash = hash(content);

        if (notSameHash(existingContentHash, newContentHash) || !existingPage.getTitle().equals(pageName)) {
            log.info("Updating page {}", pageName);
            this.confluenceClient.deletePropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY);
            int newPageVersion = existingPage.getVersion() + 1;
            this.confluenceClient.updatePage(contentId, ancestorId, pageName, content, newPageVersion, VERSION_MESSAGE, false);
            this.confluenceClient.setPropertyByKey(contentId, CONTENT_HASH_PROPERTY_KEY, newContentHash);
        } else {
            log.info("Page {} is up-to-date", pageName);
        }
    }

    public int deleteOrphanPages(String rootPageId, Set<String> generatedPageIds) {
        List<ConfluencePage> allChildPages = getAllChildPages(rootPageId);

        List<ConfluencePage> orphans = allChildPages.stream()
                .filter(page -> !generatedPageIds.contains(page.getContentId()))
                .toList();
        log.info("Deleting orphan pages: {}", orphanListAsString(orphans));
        orphans.forEach(page -> confluenceClient.deletePage(page.getContentId()));
        return orphans.size();
    }

    private List<ConfluencePage> getAllChildPages(String pageId) {
        // Find all direct child pages
        List<ConfluencePage> childPages = new ArrayList<>(
                confluenceClient.getChildPages(pageId));

        // Recurse into children of children
        childPages.addAll(childPages.stream()
                .flatMap(page -> getAllChildPages(page.getContentId()).stream())
                .collect(toSet()));

        return childPages;
    }

    private static String orphanListAsString(List<ConfluencePage> orphans) {
        return orphans.stream()
                .map(page -> page.getTitle() + " (" + page.getContentId() + ")")
                .collect(Collectors.joining(", "));
    }

    private static boolean notSameHash(String actualHash, String newHash) {
        return actualHash == null || !actualHash.equals(newHash);
    }

    private static String hash(String content) {
        return sha256Hex(content);
    }
}
