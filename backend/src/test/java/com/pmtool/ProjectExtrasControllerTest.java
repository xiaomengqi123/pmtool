package com.pmtool;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ProjectExtrasControllerTest {
    @Test
    void rejectsBlankProjectDocumentTitle() {
        PmToolService service = mock(PmToolService.class);
        Project project = new Project();
        project.id = 1L;
        when(service.project(1L)).thenReturn(project);
        when(service.fail(eq(40001), eq(HttpStatus.BAD_REQUEST), eq("项目文档标题不能为空")))
            .thenReturn(new BusinessException(40001, HttpStatus.BAD_REQUEST, "项目文档标题不能为空"));
        ProjectExtrasController controller = new ProjectExtrasController(service, mock(ProjectDocumentRepository.class), mock(ProjectRiskRepository.class));

        assertThatThrownBy(() -> controller.saveDocument(1L, new ProjectDocumentInput(" ", null)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("项目文档标题不能为空");
    }
}
