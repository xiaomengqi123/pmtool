package com.pmtool;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/operation-logs")
class AuditController {
    private final PmToolService service; private final OperationLogRepository logs; private final UserRepository users;
    AuditController(PmToolService service,OperationLogRepository logs,UserRepository users){this.service=service;this.logs=logs;this.users=users;}
    @GetMapping ApiResponse<?> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){service.requireAdmin();var result=logs.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0,page-1),Math.min(100,Math.max(1,pageSize))));return ApiResponse.ok(Map.of("items",result.getContent().stream().map(this::view).toList(),"total",result.getTotalElements(),"page",result.getNumber()+1,"pageSize",result.getSize()));}
    private Map<String,Object> view(OperationLog log){String name=log.userId==null?"系统":users.findById(log.userId).map(u->u.displayName).orElse("已删除用户");return Map.of("id",log.id,"userId",Optional.ofNullable(log.userId).orElse(0L),"userName",name,"action",log.action,"resourceType",log.resourceType,"resourceId",Optional.ofNullable(log.resourceId).orElse(0L),"detail",Optional.ofNullable(log.detail).orElse(""),"createdAt",log.createdAt.toString());}
}
