package com.pmtool;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

record LoginBody(@NotBlank String username, @NotBlank String password) {}
record PasswordBody(@NotBlank String oldPassword, @NotBlank String newPassword) {}
record StatusBody(@NotBlank String status, Long version) {}
record ReorderBody(List<Long> taskIds) {}
record DependencyBody(Long dependsOnTaskId) {}
record MemberBody(Long userId, String roleCode) {}

@RestController
@RequestMapping("/api/v1")
public class PmToolController {
    private final PmToolService service; private final UserRepository users; private final CustomerRepository customers; private final ProjectRepository projects; private final TaskRepository tasks; private final WorkLogRepository logs; private final NotificationRepository notifications;
    PmToolController(PmToolService service,UserRepository users,CustomerRepository customers,ProjectRepository projects,TaskRepository tasks,WorkLogRepository logs,NotificationRepository notifications){this.service=service;this.users=users;this.customers=customers;this.projects=projects;this.tasks=tasks;this.logs=logs;this.notifications=notifications;}
    @PostMapping("/auth/login") ApiResponse<?> login(@Valid @RequestBody LoginBody body){return ApiResponse.ok(service.login(new LoginRequest(body.username(),body.password())));}
    @GetMapping("/auth/info") ApiResponse<?> info(){return ApiResponse.ok(service.me());}
    @PostMapping("/auth/change-password") ApiResponse<Void> password(@Valid @RequestBody PasswordBody body){service.changePassword(new PasswordRequest(body.oldPassword(),body.newPassword()));return ApiResponse.ok(null);}

    @GetMapping("/users") ApiResponse<?> users(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(page(users.findByDeletedFalse(PageRequest.of(page-1,pageSize)).map(service::userView)));}
    @GetMapping("/users/all") ApiResponse<?> allUsers(){return ApiResponse.ok(service.allUsers());}
    @PostMapping("/users") ApiResponse<?> addUser(@RequestBody UserInput body){return ApiResponse.ok(service.userView(service.createUser(body)));}

    @GetMapping("/customers") ApiResponse<?> customers(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(page(customers.findByDeletedFalse(PageRequest.of(page-1,pageSize)).map(this::customerView)));}
    @PostMapping("/customers") ApiResponse<?> addCustomer(@RequestBody CustomerInput body){return ApiResponse.ok(customerView(service.saveCustomer(body,null)));}
    @PutMapping("/customers/{id}") ApiResponse<?> updateCustomer(@PathVariable Long id,@RequestBody CustomerInput body){return ApiResponse.ok(customerView(service.saveCustomer(body,id)));}
    @DeleteMapping("/customers/{id}") ApiResponse<Void> removeCustomer(@PathVariable Long id){Customer c=service.customer(id);service.requireManager();c.deleted=true;customers.save(c);return ApiResponse.ok(null);}

    @GetMapping("/projects") ApiResponse<?> projects(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(page(service.visibleProjects().stream().map(service::projectView).toList(),page,pageSize));}
    @GetMapping("/projects/{id}") ApiResponse<?> project(@PathVariable Long id){Project p=service.project(id);service.ensureProjectAccess(p,service.current());return ApiResponse.ok(service.projectView(p));}
    @PostMapping("/projects") ApiResponse<?> addProject(@RequestBody ProjectInput body){return ApiResponse.ok(service.projectView(service.saveProject(body,null)));}
    @PutMapping("/projects/{id}") ApiResponse<?> updateProject(@PathVariable Long id,@RequestBody ProjectInput body){return ApiResponse.ok(service.projectView(service.saveProject(body,id)));}
    @DeleteMapping("/projects/{id}") ApiResponse<Void> removeProject(@PathVariable Long id){Project p=service.project(id);service.ensureProjectManager(p,service.current());p.deleted=true;projects.save(p);return ApiResponse.ok(null);}
    @PostMapping("/projects/{id}/members") ApiResponse<Void> addMember(@PathVariable Long id,@RequestBody MemberBody body){service.addMember(id,body.userId(),body.roleCode());return ApiResponse.ok(null);}
    @GetMapping("/projects/{id}/tasks") ApiResponse<?> projectTasks(@PathVariable Long id){Project p=service.project(id);service.ensureProjectAccess(p,service.current());return ApiResponse.ok(tasks.findByProjectIdAndDeletedFalseOrderBySortOrderAsc(id).stream().map(this::taskView).toList());}

    @GetMapping("/tasks") ApiResponse<?> taskList(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(page(service.visibleTasks().stream().map(this::taskView).toList(),page,pageSize));}
    @PostMapping("/tasks") ApiResponse<?> addTask(@RequestBody TaskInput body){return ApiResponse.ok(taskView(service.saveTask(body,null)));}
    @PutMapping("/tasks/{id}") ApiResponse<?> updateTask(@PathVariable Long id,@RequestBody TaskInput body){return ApiResponse.ok(taskView(service.saveTask(body,id)));}
    @PatchMapping("/tasks/{id}/status") ApiResponse<Void> taskStatus(@PathVariable Long id,@RequestBody StatusBody body){service.updateTaskStatus(id,body.status(),body.version());return ApiResponse.ok(null);}
    @PostMapping("/tasks/batch-status") ApiResponse<Void> batchStatus(@RequestBody BatchTaskStatusInput body){service.batchUpdateStatus(body);return ApiResponse.ok(null);}
    @GetMapping("/tasks/{id}/dependencies") ApiResponse<?> dependencies(@PathVariable Long id){return ApiResponse.ok(service.dependenciesOf(id));}
    @PostMapping("/tasks/{id}/dependencies") ApiResponse<Void> addDependency(@PathVariable Long id,@RequestBody DependencyBody body){if(body.dependsOnTaskId()==null)throw service.fail(40001,HttpStatus.BAD_REQUEST,"依赖任务不能为空");service.addDependency(id,body.dependsOnTaskId());return ApiResponse.ok(null);}
    @DeleteMapping("/tasks/{id}/dependencies/{dependsOnTaskId}") ApiResponse<Void> removeDependency(@PathVariable Long id,@PathVariable Long dependsOnTaskId){service.removeDependency(id,dependsOnTaskId);return ApiResponse.ok(null);}
    @PostMapping("/tasks/project/{projectId}/reorder") ApiResponse<Void> reorder(@PathVariable Long projectId,@RequestBody ReorderBody body){service.reorder(projectId,body.taskIds());return ApiResponse.ok(null);}
    @GetMapping("/projects/{id}/gantt") ApiResponse<?> gantt(@PathVariable Long id){return ApiResponse.ok(service.gantt(id));}

    @GetMapping("/work-logs") ApiResponse<?> workLogs(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return ApiResponse.ok(page(service.visibleWorkLogs().stream().map(this::workLogView).toList(),page,pageSize));}
    @PostMapping("/work-logs") ApiResponse<?> addWorkLog(@RequestBody WorkLogInput body){return ApiResponse.ok(workLogView(service.saveWorkLog(body,null)));}
    @PutMapping("/work-logs/{id}") ApiResponse<?> updateWorkLog(@PathVariable Long id,@RequestBody WorkLogInput body){return ApiResponse.ok(workLogView(service.saveWorkLog(body,id)));}
    @PostMapping("/work-logs/{id}/approve") ApiResponse<Void> approve(@PathVariable Long id){service.reviewWorkLog(id,true,null);return ApiResponse.ok(null);}
    @PostMapping("/work-logs/{id}/reject") ApiResponse<Void> reject(@PathVariable Long id,@RequestBody(required=false) Map<String,String> body){service.reviewWorkLog(id,false,body==null?null:body.get("comment"));return ApiResponse.ok(null);}

    @GetMapping("/notifications") ApiResponse<?> notifications(){return ApiResponse.ok(notifications.findByUserIdOrderByCreatedAtDesc(service.current().id()).stream().map(n->Map.of("id",n.id,"title",n.title,"content",n.content,"type",n.type,"read",n.read,"createdAt",n.createdAt)).toList());}
    @GetMapping("/notifications/unread-count") ApiResponse<?> unread(){return ApiResponse.ok(Map.of("count",notifications.countByUserIdAndReadFalse(service.current().id())));}
    @PostMapping("/notifications/{id}/read") ApiResponse<Void> read(@PathVariable Long id){NotificationItem n=notifications.findById(id).orElseThrow(()->service.fail(40400,HttpStatus.NOT_FOUND,"通知不存在"));if(!Objects.equals(n.userId,service.current().id()))throw service.fail(40300,HttpStatus.FORBIDDEN,"无权限");n.read=true;notifications.save(n);return ApiResponse.ok(null);}
    @GetMapping("/dashboard") ApiResponse<?> dashboard(){return ApiResponse.ok(service.dashboard());}

    private <T> Map<String,Object> page(org.springframework.data.domain.Page<T> p){return Map.of("items",p.getContent(),"total",p.getTotalElements(),"page",p.getNumber()+1,"pageSize",p.getSize());}
    private <T> Map<String,Object> page(List<T> all,int page,int pageSize){int safePage=Math.max(1,page),safeSize=Math.min(100,Math.max(1,pageSize)),from=Math.min(all.size(),(safePage-1)*safeSize),to=Math.min(all.size(),from+safeSize);return Map.of("items",all.subList(from,to),"total",all.size(),"page",safePage,"pageSize",safeSize);}
    private Map<String,Object> customerView(Customer c){return Map.of("id",c.id,"name",c.name,"level",c.level,"status",c.status,"contactName",Optional.ofNullable(c.contactName).orElse(""),"phone",Optional.ofNullable(c.phone).orElse(""));}
    private Map<String,Object> taskView(TaskItem t){return Map.of("id",t.id,"projectId",t.projectId,"title",t.title,"assigneeId",Optional.ofNullable(t.assigneeId).orElse(0L),"status",t.status,"priority",t.priority,"estimatedHours",Optional.ofNullable(t.estimatedHours).orElse(java.math.BigDecimal.ZERO),"progress",t.progress,"sortOrder",t.sortOrder,"version",t.version==null?0:t.version);}
    private Map<String,Object> workLogView(WorkLog w){return Map.of("id",w.id,"taskId",w.taskId,"userId",w.userId,"hours",w.hours,"workDate",w.workDate,"description",Optional.ofNullable(w.description).orElse(""),"status",w.status,"reviewerId",Optional.ofNullable(w.reviewerId).orElse(0L),"version",w.version==null?0:w.version);}
}
