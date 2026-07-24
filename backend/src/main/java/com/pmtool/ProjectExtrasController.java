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
    @PostMapping("/documents") ApiResponse<?> saveDocument(@PathVariable Long projectId,@RequestBody ProjectDocumentInput body){manager(projectId);ProjectDocument d=new ProjectDocument();d.projectId=projectId;applyDocument(d,body);documents.save(d);service.log("CREATE","PROJECT_DOCUMENT",d.id,d.title);return ApiResponse.ok(Map.of("id",d.id));}
    @PutMapping("/documents/{id}") ApiResponse<?> updateDocument(@PathVariable Long projectId,@PathVariable Long id,@RequestBody ProjectDocumentInput body){manager(projectId);ProjectDocument d=document(projectId,id);applyDocument(d,body);documents.save(d);service.log("UPDATE","PROJECT_DOCUMENT",id,d.title);return ApiResponse.ok(Map.of("id",d.id));}
    @DeleteMapping("/documents/{id}") ApiResponse<Void> deleteDocument(@PathVariable Long projectId,@PathVariable Long id){manager(projectId);ProjectDocument d=document(projectId,id);d.deleted=true;documents.save(d);service.log("DELETE","PROJECT_DOCUMENT",id,d.title);return ApiResponse.ok(null);}
    @GetMapping("/risks") ApiResponse<?> risks(@PathVariable Long projectId){access(projectId);return ApiResponse.ok(risks.findByProjectIdAndDeletedFalse(projectId).stream().map(r->Map.of("id",r.id,"title",r.title,"level",r.level,"status",r.status,"ownerId",Optional.ofNullable(r.ownerId).orElse(0L))).toList());}
    @PostMapping("/risks") ApiResponse<?> saveRisk(@PathVariable Long projectId,@RequestBody ProjectRiskInput body){Project project=manager(projectId);ProjectRisk r=new ProjectRisk();r.projectId=projectId;applyRisk(project,r,body);risks.save(r);service.log("CREATE","PROJECT_RISK",r.id,r.title);return ApiResponse.ok(Map.of("id",r.id));}
    @PutMapping("/risks/{id}") ApiResponse<?> updateRisk(@PathVariable Long projectId,@PathVariable Long id,@RequestBody ProjectRiskInput body){Project project=manager(projectId);ProjectRisk r=risk(projectId,id);applyRisk(project,r,body);risks.save(r);service.log("UPDATE","PROJECT_RISK",id,r.title);return ApiResponse.ok(Map.of("id",r.id));}
    @DeleteMapping("/risks/{id}") ApiResponse<Void> deleteRisk(@PathVariable Long projectId,@PathVariable Long id){manager(projectId);ProjectRisk r=risk(projectId,id);r.deleted=true;risks.save(r);service.log("DELETE","PROJECT_RISK",id,r.title);return ApiResponse.ok(null);}
    private void access(Long id){Project p=service.project(id);service.ensureProjectAccess(p,service.current());}
    private Project manager(Long id){Project p=service.project(id);service.ensureProjectManager(p,service.current());return p;}
    private void applyDocument(ProjectDocument document,ProjectDocumentInput input){if(input.title()==null||input.title().isBlank())throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"项目文档标题不能为空");document.title=input.title();document.content=input.content();}
    private void applyRisk(Project project,ProjectRisk risk,ProjectRiskInput input){if(input.title()==null||input.title().isBlank())throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"风险标题不能为空");risk.title=input.title();risk.level=valid(input.level(),Set.of("low","medium","high"),"medium","风险等级无效");risk.status=valid(input.status(),Set.of("open","mitigating","closed"),"open","风险状态无效");service.ensureProjectParticipant(project,input.ownerId(),"风险负责人必须是项目成员");risk.ownerId=input.ownerId();}
    private String valid(String value,Set<String> allowed,String defaultValue,String message){String result=Optional.ofNullable(value).orElse(defaultValue);if(!allowed.contains(result))throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,message);return result;}
    private ProjectDocument document(Long projectId,Long id){return documents.findById(id).filter(d->!d.deleted&&Objects.equals(d.projectId,projectId)).orElseThrow(()->service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"项目文档不存在"));}
    private ProjectRisk risk(Long projectId,Long id){return risks.findById(id).filter(r->!r.deleted&&Objects.equals(r.projectId,projectId)).orElseThrow(()->service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"项目风险不存在"));}
}
