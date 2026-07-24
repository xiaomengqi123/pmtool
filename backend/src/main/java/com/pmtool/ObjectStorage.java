package com.pmtool;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.InputStream;
import java.util.*;

@Configuration
class MinioConfig {
    @Bean MinioClient minioClient(@Value("${pmtool.minio.endpoint}") String endpoint,@Value("${pmtool.minio.access-key}") String access,@Value("${pmtool.minio.secret-key}") String secret){return MinioClient.builder().endpoint(endpoint).credentials(access,secret).build();}
}
@Service
class AttachmentService {
    private final MinioClient minio; private final AttachmentRepository attachments; private final String bucket; private final PmToolService service;
    AttachmentService(MinioClient minio,AttachmentRepository attachments,@Value("${pmtool.minio.bucket}")String bucket,PmToolService service){this.minio=minio;this.attachments=attachments;this.bucket=bucket;this.service=service;}
    Attachment upload(String type,Long targetId,MultipartFile file){if(file.isEmpty()||file.getSize()>2*1024*1024)throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"文件必须大于 0 且不超过 2 MB");try{if(!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());String key=type+"/"+targetId+"/"+UUID.randomUUID();minio.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(file.getInputStream(),file.getSize(),-1).contentType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream")).build());Attachment a=new Attachment();a.targetType=type;a.targetId=targetId;a.originalName=file.getOriginalFilename();a.objectKey=key;a.contentType=file.getContentType();a.sizeBytes=file.getSize();a.uploaderId=service.current().id();attachments.save(a);service.log("CREATE","ATTACHMENT",a.id,a.originalName);return a;}catch(Exception e){throw service.fail(50000,org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,"文件上传失败");}}
    Attachment get(Long id){return attachments.findById(id).filter(a->!a.deleted).orElseThrow(()->service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"附件不存在"));}
    InputStream stream(Attachment a){try{return minio.getObject(GetObjectArgs.builder().bucket(bucket).object(a.objectKey).build());}catch(Exception e){throw service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"对象存储文件不存在");}}
}
@RestController @RequestMapping("/api/v1/attachments")
class AttachmentController {
    private final AttachmentService service;private final AttachmentRepository repository;private final PmToolService pmTool;
    AttachmentController(AttachmentService service,AttachmentRepository repository,PmToolService pmTool){this.service=service;this.repository=repository;this.pmTool=pmTool;}
    @GetMapping("/{targetType}/{targetId}") ApiResponse<?> list(@PathVariable String targetType,@PathVariable Long targetId){authorize(targetType,targetId);return ApiResponse.ok(repository.findByTargetTypeAndTargetIdAndDeletedFalse(targetType,targetId).stream().map(this::view).toList());}
    @PostMapping("/{targetType}/{targetId}") ApiResponse<?> upload(@PathVariable String targetType,@PathVariable Long targetId,@RequestParam("file") MultipartFile file){authorize(targetType,targetId);return ApiResponse.ok(view(service.upload(targetType,targetId,file)));}
    @GetMapping("/{id}/download") ResponseEntity<StreamingResponseBody> download(@PathVariable Long id){Attachment a=service.get(id);authorize(a.targetType,a.targetId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(Optional.ofNullable(a.contentType).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE))).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+a.originalName.replace("\"","")+"\"").body(out->{try(InputStream in=service.stream(a)){in.transferTo(out);}});}
    @DeleteMapping("/{id}") ApiResponse<Void> delete(@PathVariable Long id){Attachment a=service.get(id);authorize(a.targetType,a.targetId);if(!Objects.equals(a.uploaderId,pmTool.current().id()))requireTargetManager(a.targetType,a.targetId);a.deleted=true;repository.save(a);pmTool.log("DELETE","ATTACHMENT",a.id,a.originalName);return ApiResponse.ok(null);}
    private void authorize(String targetType,Long targetId){if("project".equals(targetType)){pmTool.ensureProjectAccess(pmTool.project(targetId),pmTool.current());return;}if("task".equals(targetType)){TaskItem task=pmTool.task(targetId);pmTool.ensureProjectAccess(pmTool.project(task.projectId),pmTool.current());return;}throw pmTool.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"不支持的附件归属类型");}
    private void requireTargetManager(String targetType,Long targetId){if("project".equals(targetType)){pmTool.ensureProjectManager(pmTool.project(targetId),pmTool.current());return;}TaskItem task=pmTool.task(targetId);pmTool.ensureProjectManager(pmTool.project(task.projectId),pmTool.current());}
    private Map<String,Object> view(Attachment a){return Map.of("id",a.id,"name",a.originalName,"contentType",Optional.ofNullable(a.contentType).orElse("application/octet-stream"),"size",a.sizeBytes,"createdAt",a.createdAt);}
}
