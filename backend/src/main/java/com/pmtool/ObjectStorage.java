package com.pmtool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Service
class AttachmentService {
    private final Path root; private final AttachmentRepository attachments; private final PmToolService service;
    AttachmentService(@Value("${pmtool.storage.local-dir}") String localDir,AttachmentRepository attachments,PmToolService service){this.root=Path.of(localDir).toAbsolutePath().normalize();this.attachments=attachments;this.service=service;}
    Attachment upload(String type,Long targetId,MultipartFile file){if(file.isEmpty()||file.getSize()>2*1024*1024)throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"文件必须大于 0 且不超过 2 MB");try{String key=type+"/"+targetId+"/"+UUID.randomUUID(),originalName=Optional.ofNullable(file.getOriginalFilename()).filter(name->!name.isBlank()).orElse("file");Path target=objectPath(key);Files.createDirectories(target.getParent());try(InputStream input=file.getInputStream()){Files.copy(input,target,StandardCopyOption.REPLACE_EXISTING);}Attachment a=new Attachment();a.targetType=type;a.targetId=targetId;a.originalName=originalName;a.objectKey=key;a.contentType=file.getContentType();a.sizeBytes=file.getSize();a.uploaderId=service.current().id();attachments.save(a);service.log("CREATE","ATTACHMENT",a.id,a.originalName);return a;}catch(IOException|SecurityException e){throw service.fail(50000,org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,"文件上传失败");}}
    Attachment get(Long id){return attachments.findById(id).filter(a->!a.deleted).orElseThrow(()->service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"附件不存在"));}
    InputStream stream(Attachment a){try{return Files.newInputStream(objectPath(a.objectKey));}catch(IOException|SecurityException e){throw service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"附件文件不存在");}}
    private Path objectPath(String key){Path target=root.resolve(key).normalize();if(!target.startsWith(root))throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"附件路径无效");return target;}
}
@RestController @RequestMapping("/api/v1/attachments")
class AttachmentController {
    private final AttachmentService service;private final AttachmentRepository repository;private final PmToolService pmTool;
    AttachmentController(AttachmentService service,AttachmentRepository repository,PmToolService pmTool){this.service=service;this.repository=repository;this.pmTool=pmTool;}
    @GetMapping("/{targetType}/{targetId}") ApiResponse<?> list(@PathVariable String targetType,@PathVariable Long targetId){authorize(targetType,targetId);return ApiResponse.ok(repository.findByTargetTypeAndTargetIdAndDeletedFalse(targetType,targetId).stream().map(this::view).toList());}
    @PostMapping("/{targetType}/{targetId}") ApiResponse<?> upload(@PathVariable String targetType,@PathVariable Long targetId,@RequestParam("file") MultipartFile file){authorize(targetType,targetId);return ApiResponse.ok(view(service.upload(targetType,targetId,file)));}
    @GetMapping("/{id}/download") ResponseEntity<StreamingResponseBody> download(@PathVariable Long id){Attachment a=service.get(id);authorize(a.targetType,a.targetId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(Optional.ofNullable(a.contentType).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE))).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+safeFileName(a.originalName)+"\"").body(out->{try(InputStream in=service.stream(a)){in.transferTo(out);}});}
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable Long id){Attachment a=service.get(id);authorize(a.targetType,a.targetId);if(!Objects.equals(a.uploaderId,pmTool.current().id()))requireTargetManager(a.targetType,a.targetId);a.deleted=true;repository.save(a);pmTool.log("DELETE","ATTACHMENT",a.id,a.originalName);return ApiResponse.ok(null);}
    private void authorize(String targetType,Long targetId){if("project".equals(targetType)){pmTool.ensureProjectAccess(pmTool.project(targetId),pmTool.current());return;}if("task".equals(targetType)){TaskItem task=pmTool.task(targetId);pmTool.ensureProjectAccess(pmTool.project(task.projectId),pmTool.current());return;}throw pmTool.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"不支持的附件归属类型");}
    private void requireTargetManager(String targetType,Long targetId){if("project".equals(targetType)){pmTool.ensureProjectManager(pmTool.project(targetId),pmTool.current());return;}TaskItem task=pmTool.task(targetId);pmTool.ensureProjectManager(pmTool.project(task.projectId),pmTool.current());}
    static String safeFileName(String name){return Optional.ofNullable(name).orElse("file").replaceAll("[\\r\\n\"]","");}
    private Map<String,Object> view(Attachment a){return Map.of("id",a.id,"name",a.originalName,"contentType",Optional.ofNullable(a.contentType).orElse("application/octet-stream"),"size",a.sizeBytes,"uploaderId",a.uploaderId,"createdAt",a.createdAt);}
}
