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
record ResetPasswordBody(@NotBlank String password) {}

@RestController
@RequestMapping("/api/v1")
public class PmToolController {
    private final PmToolService service; private final UserRepository users; private final CustomerRepository customers; private final ProjectRepository projects; private final TaskRepository tasks; private final WorkLogRepository logs; private final NotificationRepository notifications;
    PmToolController(PmToolService service,UserRepository users,CustomerRepository customers,ProjectRepository projects,TaskRepository tasks,WorkLogRepository logs,NotificationRepository notifications){this.service=service;this.users=users;this.customers=customers;this.projects=projects;this.tasks=tasks;this.logs=logs;this.notifications=notifications;}
    @PostMapping("/auth/login") ApiResponse<?> login(@Valid @RequestBody LoginBody body){return ApiResponse.ok(service.login(new LoginRequest(body.username(),body.password())));}
    @GetMapping("/auth/info") ApiResponse<?> info(){return ApiResponse.ok(service.me());}
    @PostMapping("/auth/change-password") ApiResponse<Void> password(@Valid @RequestBody PasswordBody body){service.changePassword(new PasswordRequest(body.oldPassword(),body.newPassword()));return ApiResponse.ok(null);}

    @GetMapping("/users") ApiResponse<?> users(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String keyword,@RequestParam(required=false)String roleCode){service.requireAdmin();List<UserAccount> filtered=users.findByDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent().stream().filter(u->(matchesText(u.username,keyword)||matchesText(u.displayName,keyword))&&matchesValue(u.roleCode,roleCode)).toList();return ApiResponse.ok(page(filtered.stream().map(service::userView).toList(),page,pageSize));}
    @GetMapping("/users/all") ApiResponse<?> allUsers(){return ApiResponse.ok(service.allUsers());}
    @PostMapping("/users") ApiResponse<?> addUser(@RequestBody UserInput body){return ApiResponse.ok(service.userView(service.createUser(body)));}
    @PutMapping("/users/{id}") ApiResponse<?> updateUser(@PathVariable Long id,@RequestBody UserUpdateInput body){return ApiResponse.ok(service.userView(service.updateUser(id,body)));}
    @PostMapping("/users/{id}/reset-password") ApiResponse<Void> resetPassword(@PathVariable Long id,@Valid @RequestBody ResetPasswordBody body){service.resetPassword(id,body.password());return ApiResponse.ok(null);}

    @GetMapping("/customers") ApiResponse<?> customers(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status){service.requireManager();List<Customer> filtered=customers.findByDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent().stream().filter(c->matchesText(c.name,keyword)&&matchesValue(c.status,status)).toList();return ApiResponse.ok(page(filtered.stream().map(this::customerView).toList(),page,pageSize));}
    @PostMapping("/customers") ApiResponse<?> addCustomer(@RequestBody CustomerInput body){return ApiResponse.ok(customerView(service.saveCustomer(body,null)));}
    @PutMapping("/customers/{id}") ApiResponse<?> updateCustomer(@PathVariable Long id,@RequestBody CustomerInput body){return ApiResponse.ok(customerView(service.saveCustomer(body,id)));}
    @DeleteMapping("/customers/{id}") ApiResponse<Void> removeCustomer(@PathVariable Long id){Customer c=service.customer(id);service.requireManager();c.deleted=true;customers.save(c);return ApiResponse.ok(null);}

    @GetMapping("/projects") ApiResponse<?> projects(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status){List<Project> filtered=service.visibleProjects().stream().filter(p->(matchesText(p.name,keyword)||matchesText(p.code,keyword))&&matchesValue(p.status,status)).toList();return ApiResponse.ok(page(filtered.stream().map(service::projectView).toList(),page,pageSize));}
    @GetMapping("/projects/{id}") ApiResponse<?> project(@PathVariable Long id){Project p=service.project(id);service.ensureProjectAccess(p,service.current());return ApiResponse.ok(service.projectView(p));}
    @PostMapping("/projects") ApiResponse<?> addProject(@RequestBody ProjectInput body){return ApiResponse.ok(service.projectView(service.saveProject(body,null)));}
    @PutMapping("/projects/{id}") ApiResponse<?> updateProject(@PathVariable Long id,@RequestBody ProjectInput body){return ApiResponse.ok(service.projectView(service.saveProject(body,id)));}
    @DeleteMapping("/projects/{id}") ApiResponse<Void> removeProject(@PathVariable Long id){Project p=service.project(id);service.ensureProjectManager(p,service.current());p.deleted=true;projects.save(p);return ApiResponse.ok(null);}
    @PostMapping("/projects/{id}/members") ApiResponse<Void> addMember(@PathVariable Long id,@RequestBody MemberBody body){service.addMember(id,body.userId(),body.roleCode());return ApiResponse.ok(null);}
    @GetMapping("/projects/{id}/tasks") ApiResponse<?> projectTasks(@PathVariable Long id){Project p=service.project(id);service.ensureProjectAccess(p,service.current());return ApiResponse.ok(tasks.findByProjectIdAndDeletedFalseOrderBySortOrderAsc(id).stream().map(this::taskView).toList());}

    @GetMapping("/tasks") ApiResponse<?> taskList(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String keyword,@RequestParam(required=false)String status,@RequestParam(required=false)Long projectId,@RequestParam(required=false)Long assigneeId){List<TaskItem> filtered=service.visibleTasks().stream().filter(t->(matchesText(t.title,keyword)||matchesText(t.description,keyword))&&matchesValue(t.status,status)&&(projectId==null||Objects.equals(t.projectId,projectId))&&(assigneeId==null||Objects.equals(t.assigneeId,assigneeId))).toList();return ApiResponse.ok(page(filtered.stream().map(this::taskView).toList(),page,pageSize));}
    @PostMapping("/tasks") ApiResponse<?> addTask(@RequestBody TaskInput body){return ApiResponse.ok(taskView(service.saveTask(body,null)));}
    @PutMapping("/tasks/{id}") ApiResponse<?> updateTask(@PathVariable Long id,@RequestBody TaskInput body){return ApiResponse.ok(taskView(service.saveTask(body,id)));}
    @DeleteMapping("/tasks/{id}") ApiResponse<Void> removeTask(@PathVariable Long id){service.deleteTask(id);return ApiResponse.ok(null);}
    @PatchMapping("/tasks/{id}/status") ApiResponse<Void> taskStatus(@PathVariable Long id,@RequestBody StatusBody body){service.updateTaskStatus(id,body.status(),body.version());return ApiResponse.ok(null);}
    @PostMapping("/tasks/batch-status") ApiResponse<Void> batchStatus(@RequestBody BatchTaskStatusInput body){service.batchUpdateStatus(body);return ApiResponse.ok(null);}
    @GetMapping("/tasks/{id}/dependencies") ApiResponse<?> dependencies(@PathVariable Long id){return ApiResponse.ok(service.dependenciesOf(id));}
    @PostMapping("/tasks/{id}/dependencies") ApiResponse<Void> addDependency(@PathVariable Long id,@RequestBody DependencyBody body){if(body.dependsOnTaskId()==null)throw service.fail(40001,HttpStatus.BAD_REQUEST,"依赖任务不能为空");service.addDependency(id,body.dependsOnTaskId());return ApiResponse.ok(null);}
    @DeleteMapping("/tasks/{id}/dependencies/{dependsOnTaskId}") ApiResponse<Void> removeDependency(@PathVariable Long id,@PathVariable Long dependsOnTaskId){service.removeDependency(id,dependsOnTaskId);return ApiResponse.ok(null);}
    @PostMapping("/tasks/project/{projectId}/reorder") ApiResponse<Void> reorder(@PathVariable Long projectId,@RequestBody ReorderBody body){service.reorder(projectId,body.taskIds());return ApiResponse.ok(null);}
    @GetMapping("/projects/{id}/gantt") ApiResponse<?> gantt(@PathVariable Long id){return ApiResponse.ok(service.gantt(id));}

    @GetMapping("/work-logs") ApiResponse<?> workLogs(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize,@RequestParam(required=false)String status,@RequestParam(required=false)Long taskId){List<WorkLog> filtered=service.visibleWorkLogs().stream().filter(w->matchesValue(w.status,status)&&(taskId==null||Objects.equals(w.taskId,taskId))).toList();return ApiResponse.ok(page(filtered.stream().map(this::workLogView).toList(),page,pageSize));}
    @PostMapping("/work-logs") ApiResponse<?> addWorkLog(@RequestBody WorkLogInput body){return ApiResponse.ok(workLogView(service.saveWorkLog(body,null)));}
    @PutMapping("/work-logs/{id}") ApiResponse<?> updateWorkLog(@PathVariable Long id,@RequestBody WorkLogInput body){return ApiResponse.ok(workLogView(service.saveWorkLog(body,id)));}
    @PostMapping("/work-logs/{id}/approve") ApiResponse<Void> approve(@PathVariable Long id){service.reviewWorkLog(id,true,null);return ApiResponse.ok(null);}
    @PostMapping("/work-logs/{id}/reject") ApiResponse<Void> reject(@PathVariable Long id,@RequestBody(required=false) Map<String,String> body){service.reviewWorkLog(id,false,body==null?null:body.get("comment"));return ApiResponse.ok(null);}

    @GetMapping("/notifications") ApiResponse<?> notifications(){return ApiResponse.ok(notifications.findByUserIdOrderByCreatedAtDesc(service.current().id()).stream().map(n->Map.of("id",n.id,"title",n.title,"content",n.content,"type",n.type,"read",n.read,"createdAt",n.createdAt)).toList());}
    @GetMapping("/notifications/unread-count") ApiResponse<?> unread(){return ApiResponse.ok(Map.of("count",notifications.countByUserIdAndReadFalse(service.current().id())));}
    @PostMapping("/notifications/{id}/read") ApiResponse<Void> read(@PathVariable Long id){NotificationItem n=notifications.findById(id).orElseThrow(()->service.fail(40400,HttpStatus.NOT_FOUND,"通知不存在"));if(!Objects.equals(n.userId,service.current().id()))throw service.fail(40300,HttpStatus.FORBIDDEN,"无权限");n.read=true;notifications.save(n);return ApiResponse.ok(null);}
    @PostMapping("/notifications/read-all") ApiResponse<Void> readAll(){Long userId=service.current().id();notifications.findByUserIdOrderByCreatedAtDesc(userId).stream().filter(n->!n.read).forEach(n->{n.read=true;notifications.save(n);});return ApiResponse.ok(null);}
    @GetMapping("/dashboard") ApiResponse<?> dashboard(){return ApiResponse.ok(service.dashboard());}

    private <T> Map<String,Object> page(org.springframework.data.domain.Page<T> p){return Map.of("items",p.getContent(),"total",p.getTotalElements(),"page",p.getNumber()+1,"pageSize",p.getSize());}
    private <T> Map<String,Object> page(List<T> all,int page,int pageSize){int safePage=Math.max(1,page),safeSize=Math.min(100,Math.max(1,pageSize)),from=Math.min(all.size(),(safePage-1)*safeSize),to=Math.min(all.size(),from+safeSize);return Map.of("items",all.subList(from,to),"total",all.size(),"page",safePage,"pageSize",safeSize);}
    private boolean matchesText(String value,String keyword){return keyword==null||keyword.isBlank()||Optional.ofNullable(value).orElse("").toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));}
    private boolean matchesValue(String value,String expected){return expected==null||expected.isBlank()||Objects.equals(value,expected);}
    private Map<String,Object> customerView(Customer c){return Map.of("id",c.id,"name",c.name,"level",c.level,"status",c.status,"contactName",Optional.ofNullable(c.contactName).orElse(""),"phone",Optional.ofNullable(c.phone).orElse(""));}
    private Map<String,Object> taskView(TaskItem t){Map<String,Object> result=new LinkedHashMap<>();result.put("id",t.id);result.put("projectId",t.projectId);result.put("title",t.title);result.put("description",Optional.ofNullable(t.description).orElse(""));result.put("assigneeId",Optional.ofNullable(t.assigneeId).orElse(0L));result.put("status",t.status);result.put("priority",t.priority);result.put("estimatedHours",Optional.ofNullable(t.estimatedHours).orElse(java.math.BigDecimal.ZERO));result.put("progress",t.progress);result.put("sortOrder",t.sortOrder);result.put("startDate",Optional.ofNullable(t.startDate).map(Object::toString).orElse(""));result.put("dueDate",Optional.ofNullable(t.dueDate).map(Object::toString).orElse(""));result.put("version",t.version==null?0:t.version);return result;}
    private Map<String,Object> workLogView(WorkLog w){Map<String,Object> result=new LinkedHashMap<>();result.put("id",w.id);result.put("taskId",w.taskId);result.put("userId",w.userId);result.put("hours",w.hours);result.put("workDate",w.workDate);result.put("description",Optional.ofNullable(w.description).orElse(""));result.put("status",w.status);result.put("reviewerId",Optional.ofNullable(w.reviewerId).orElse(0L));result.put("reviewComment",Optional.ofNullable(w.reviewComment).orElse(""));result.put("version",w.version==null?0:w.version);result.put("canManage",service.canManageWorkLog(w));return result;}
}
