package com.pmtool;

import org.springframework.web.bind.annotation.*;
import java.util.*;

record ProjectDocumentInput(String title, String content) {}
record ProjectRiskInput(String title, String level, String status, Long ownerId) {}

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class ProjectExtrasController {
    private final PmToolService service; private final ProjectDocumentRepository documents; private final ProjectRiskRepository risks;
    ProjectExtrasController(PmToolService service,ProjectDocumentRepository documents,ProjectRiskRepository risks){this.service=service;this.documents=documents;this.risks=risks;}
    @GetMapping("/documents") ApiResponse<?> documents(@PathVariable Long projectId){access(projectId);return ApiResponse.ok(documents.findByProjectIdAndDeletedFalse(projectId).stream().map(d->Map.of("id",d.id,"title",d.title,"content",Optional.ofNullable(d.content).orElse(""),"createdAt",d.createdAt)).toList());}
    @PostMapping("/documents") ApiResponse<?> saveDocument(@PathVariable Long projectId,@RequestBody ProjectDocumentInput body){manager(projectId);ProjectDocument d=new ProjectDocument();d.projectId=projectId;d.title=body.title();d.content=body.content();documents.save(d);service.log("CREATE","PROJECT_DOCUMENT",d.id,d.title);return ApiResponse.ok(Map.of("id",d.id));}
    @GetMapping("/risks") ApiResponse<?> risks(@PathVariable Long projectId){access(projectId);return ApiResponse.ok(risks.findByProjectIdAndDeletedFalse(projectId).stream().map(r->Map.of("id",r.id,"title",r.title,"level",r.level,"status",r.status,"ownerId",Optional.ofNullable(r.ownerId).orElse(0L))).toList());}
    @PostMapping("/risks") ApiResponse<?> saveRisk(@PathVariable Long projectId,@RequestBody ProjectRiskInput body){manager(projectId);ProjectRisk r=new ProjectRisk();r.projectId=projectId;r.title=body.title();r.level=Optional.ofNullable(body.level()).orElse("medium");r.status=Optional.ofNullable(body.status()).orElse("open");r.ownerId=body.ownerId();risks.save(r);service.log("CREATE","PROJECT_RISK",r.id,r.title);return ApiResponse.ok(Map.of("id",r.id));}
    private void access(Long id){Project p=service.project(id);service.ensureProjectAccess(p,service.current());}
    private void manager(Long id){Project p=service.project(id);service.ensureProjectManager(p,service.current());}
}
