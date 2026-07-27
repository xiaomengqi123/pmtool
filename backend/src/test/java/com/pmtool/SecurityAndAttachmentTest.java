package com.pmtool;

import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class SecurityAndAttachmentTest {
    @TempDir Path storageDir;
    @Test void jwtRoundTripPreservesCurrentUser() {
        UserAccount user = new UserAccount("member", "hash", "成员", "MEMBER"); user.id = 7L;
        JwtService jwt = new JwtService("test-secret-key-must-be-at-least-thirty-two-characters-long", 60);
        CurrentUser current = jwt.parse(jwt.create(user));
        assertThat(current.id()).isEqualTo(7L);
        assertThat(current.username()).isEqualTo("member");
        assertThat(current.role()).isEqualTo("MEMBER");
    }

    @Test void attachmentRejectsFilesOverTwoMegabytesBeforeStorageAccess() {
        PmToolService pmTool = mock(PmToolService.class);
        when(pmTool.fail(anyInt(), any(), any())).thenAnswer(call -> new BusinessException(call.getArgument(0), call.<org.springframework.http.HttpStatus>getArgument(1), call.<String>getArgument(2)));
        AttachmentService attachments = new AttachmentService(storageDir.toString(), mock(AttachmentRepository.class), pmTool);
        MockMultipartFile oversized = new MockMultipartFile("file", "large.bin", "application/octet-stream", new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> attachments.upload("project", 1L, oversized)).isInstanceOf(BusinessException.class).hasMessageContaining("2 MB");
    }

    @Test void attachmentUsesPrivateLocalDirectory() throws Exception {
        PmToolService pmTool = mock(PmToolService.class);
        AttachmentRepository repository = mock(AttachmentRepository.class);
        when(pmTool.current()).thenReturn(new CurrentUser(7L, "member", "MEMBER"));
        when(repository.save(any(Attachment.class))).thenAnswer(call -> { Attachment item = call.getArgument(0); item.id = 9L; return item; });
        AttachmentService attachments = new AttachmentService(storageDir.toString(), repository, pmTool);
        Attachment stored = attachments.upload("project", 12L, new MockMultipartFile("file", "plan.txt", "text/plain", "content".getBytes()));
        assertThat(stored.objectKey).startsWith("project/12/").doesNotContain("plan.txt");
        assertThat(Files.readAllBytes(storageDir.resolve(stored.objectKey))).isEqualTo("content".getBytes());
        try (var input = attachments.stream(stored)) { assertThat(input.readAllBytes()).isEqualTo("content".getBytes()); }
    }

    @Test void attachmentDownloadFileNameRemovesHeaderControlCharacters() {
        assertThat(AttachmentController.safeFileName("report\r\n\".txt")).isEqualTo("report.txt");
        assertThat(AttachmentController.safeFileName(null)).isEqualTo("file");
    }
}
