package ch.admin.bit.jeap.governance.reporting.confluence;

import ch.admin.bit.jeap.governance.reporting.ReportingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceClient;
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluencePage;
import org.sahli.asciidoc.confluence.publisher.client.http.NotFoundException;

import java.util.List;
import java.util.Set;

import static ch.admin.bit.jeap.governance.reporting.confluence.ConfluenceAdapterImpl.CONTENT_HASH_PROPERTY_KEY;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfluenceAdapterImplTest {

    private static final String SPACE_KEY = "SPACE_KEY";

    @Mock
    private ConfluenceClient confluenceClientMock;

    private ConfluenceAdapterImpl confluenceAdapter;

    @Test
    void addOrUpdatePageUnderAncestor_newPage() {
        String pageId = "pageId";
        String pageName = "pageName";
        String ancestorId = "ancestorId";
        String content = "content";
        String contentHash = sha256Hex(content);
        doThrow(new NotFoundException()).when(confluenceClientMock).getPageByTitle(SPACE_KEY, ancestorId, pageName);
        doReturn(pageId)
                .when(confluenceClientMock)
                .addPageUnderAncestor(eq(SPACE_KEY), eq(ancestorId), eq(pageName), eq(content), nullable(String.class));

        confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, pageName, content);

        verify(confluenceClientMock).setPropertyByKey(pageId, CONTENT_HASH_PROPERTY_KEY, contentHash);
    }

    @Test
    void addOrUpdatePageUnderAncestor_existingPageWithNewContent() {
        String pageId = "pageId";
        String pageName = "pageName";
        String ancestorId = "ancestorId";
        String content = "content";
        String contentHash = sha256Hex(content);
        int version = 1;
        ConfluencePage existingPage = new ConfluencePage(pageId, pageName, version);
        doReturn(pageId)
                .when(confluenceClientMock).getPageByTitle(SPACE_KEY, ancestorId, pageName);
        doReturn(existingPage)
                .when(confluenceClientMock).getPageWithContentAndVersionById(pageId);
        doReturn("differenthash")
                .when(confluenceClientMock).getPropertyByKey(pageId, CONTENT_HASH_PROPERTY_KEY);

        confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, pageName, content);

        verify(confluenceClientMock).updatePage(eq(pageId), eq(ancestorId), eq(pageName), eq(content), eq(version + 1), nullable(String.class), eq(false));
        verify(confluenceClientMock).setPropertyByKey(pageId, CONTENT_HASH_PROPERTY_KEY, contentHash);
    }

    @Test
    void addOrUpdatePageUnderAncestor_existingPageNoContentChange() {
        String pageId = "pageId";
        String pageName = "pageName";
        String ancestorId = "ancestorId";
        String content = "content";
        String contentHash = sha256Hex(content);
        int version = 1;
        ConfluencePage existingPage = new ConfluencePage(pageId, pageName, version);
        doReturn(pageId)
                .when(confluenceClientMock).getPageByTitle(SPACE_KEY, ancestorId, pageName);
        doReturn(existingPage)
                .when(confluenceClientMock).getPageWithContentAndVersionById(pageId);
        doReturn(contentHash)
                .when(confluenceClientMock).getPropertyByKey(pageId, CONTENT_HASH_PROPERTY_KEY);

        confluenceAdapter.addOrUpdatePageUnderAncestor(ancestorId, pageName, content);

        verifyNoMoreInteractions(confluenceClientMock);
    }

    @Test
    void deleteOrphanPages() {
        // Mock a page structure with child1 and child2 as generated pages, and orphan1/2 as non-generated pages:
        // root/
        // ├── child1/
        // │   ├── child2/
        // │       └── orphan1
        // └── orphan2
        ConfluencePage child1Mock = mockPage("child1");
        ConfluencePage child2Mock = mockPage("child2");
        ConfluencePage orphan1Mock = mockPage("orphan1");
        ConfluencePage orphan2Mock = mockPage("orphan2");

        doReturn(List.of(child1Mock, orphan2Mock))
                .when(confluenceClientMock).getChildPages("root");
        doReturn(List.of(child2Mock))
                .when(confluenceClientMock).getChildPages("child1");
        doReturn(List.of(orphan1Mock))
                .when(confluenceClientMock).getChildPages("child2");
        doReturn(List.of())
                .when(confluenceClientMock).getChildPages("orphan1");
        doReturn(List.of())
                .when(confluenceClientMock).getChildPages("orphan2");

        confluenceAdapter.deleteOrphanPages("root", Set.of("child1", "child2"));

        verify(confluenceClientMock).deletePage("orphan1");
        verify(confluenceClientMock).deletePage("orphan2");
    }

    private ConfluencePage mockPage(String pageId) {
        ConfluencePage page = mock(ConfluencePage.class);
        doReturn(pageId).when(page).getContentId();
        lenient().doReturn("Title " + pageId).when(page).getTitle();
        return page;
    }

    @BeforeEach
    void setUp() {
        ReportingProperties properties = new ReportingProperties();
        properties.getConfluence().setSpaceKey(SPACE_KEY);
        confluenceAdapter = new ConfluenceAdapterImpl(confluenceClientMock, properties);
    }
}
