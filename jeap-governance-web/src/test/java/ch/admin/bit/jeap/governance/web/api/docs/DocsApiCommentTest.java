package ch.admin.bit.jeap.governance.web.api.docs;

import ch.admin.bit.jeap.governance.web.api.docs.DocsApiDtoFactory.Comment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocsApiCommentTest {

    @Test
    void noComment() {
        assertThat(Comment.of(null)).isEqualTo(new Comment(null, List.of()));
        assertThat(Comment.of("  \n ")).isEqualTo(new Comment(null, List.of()));
    }

    @Test
    void singleLine_isTheSummaryWithoutDetails() {
        assertThat(Comment.of("Signature is not enforced")).isEqualTo(new Comment("Signature is not enforced", List.of()));
    }

    @Test
    void furtherLines_areTheDetails_blankOnesLeftOut() {
        assertThat(Comment.of("Calls without a JWT bearer token:\n'GET /a'\n\n  'POST /b'  \r\n'PUT /c'\n"))
                .isEqualTo(new Comment("Calls without a JWT bearer token:", List.of("'GET /a'", "'POST /b'", "'PUT /c'")));
    }

    @Test
    void markdownAndHtml_arePassedOnAsText() {
        assertThat(Comment.of("**bold** <b>x</b>\n- [link](http://x)"))
                .isEqualTo(new Comment("**bold** <b>x</b>", List.of("- [link](http://x)")));
    }
}
