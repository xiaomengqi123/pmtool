package com.pmtool;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

class SecurityAndAttachmentTest {
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
        AttachmentService attachments = new AttachmentService(mock(MinioClient.class), mock(AttachmentRepository.class), "test-bucket", pmTool);
        MockMultipartFile oversized = new MockMultipartFile("file", "large.bin", "application/octet-stream", new byte[2 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> attachments.upload("project", 1L, oversized)).isInstanceOf(BusinessException.class).hasMessageContaining("2 MB");
    }
}
