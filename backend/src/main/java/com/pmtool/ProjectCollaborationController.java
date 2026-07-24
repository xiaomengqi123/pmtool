package com.pmtool;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

record MilestoneInput(String name, LocalDateTime dueDate, String status) {}

@RestController
@RequestMapping("/api/v1/projects/{projectId}")
class ProjectCollaborationController {
    private final PmToolService service; private final ProjectMemberRepository members; private final UserRepository users; private final MilestoneRepository milestones;
    ProjectCollaborationController(PmToolService service,ProjectMemberRepository members,UserRepository users,MilestoneRepository milestones){this.service=service;this.members=members;this.users=users;this.milestones=milestones;}

    @GetMapping("/members") ApiResponse<?> members(@PathVariable Long projectId){Project project=access(projectId);return ApiResponse.ok(members.findByIdProjectId(projectId).stream().map(m->{UserAccount u=service.user(m.id.userId);return Map.of("userId",u.id,"displayName",u.displayName,"username",u.username,"roleCode",m.roleCode,"isManager",Objects.equals(project.managerId,u.id));}).toList());}
    @DeleteMapping("/members/{userId}") ApiResponse<Void> removeMember(@PathVariable Long projectId,@PathVariable Long userId){Project project=manage(projectId);if(Objects.equals(project.managerId,userId))throw service.fail(40001,HttpStatus.BAD_REQUEST,"项目经理不能移除自己");ProjectMemberId id=new ProjectMemberId(projectId,userId);if(!members.existsById(id))throw service.fail(40400,HttpStatus.NOT_FOUND,"项目成员不存在");members.deleteById(id);service.log("DELETE","PROJECT_MEMBER",userId,"移除项目成员");return ApiResponse.ok(null);}

    @GetMapping("/milestones") ApiResponse<?> milestones(@PathVariable Long projectId){access(projectId);return ApiResponse.ok(milestones.findByProjectIdAndDeletedFalseOrderByDueDateAsc(projectId).stream().map(this::view).toList());}
    @PostMapping("/milestones") ApiResponse<?> createMilestone(@PathVariable Long projectId,@RequestBody MilestoneInput input){manage(projectId);if(input.name()==null||input.name().isBlank())throw service.fail(40001,HttpStatus.BAD_REQUEST,"里程碑名称不能为空");Milestone item=new Milestone();item.projectId=projectId;apply(item,input);milestones.save(item);service.log("CREATE","MILESTONE",item.id,item.name);return ApiResponse.ok(view(item));}
    @PutMapping("/milestones/{id}") ApiResponse<?> updateMilestone(@PathVariable Long projectId,@PathVariable Long id,@RequestBody MilestoneInput input){manage(projectId);Milestone item=milestones.findById(id).filter(x->!x.deleted&&Objects.equals(x.projectId,projectId)).orElseThrow(()->service.fail(40400,HttpStatus.NOT_FOUND,"里程碑不存在"));apply(item,input);milestones.save(item);service.log("UPDATE","MILESTONE",id,item.name);return ApiResponse.ok(view(item));}
    @DeleteMapping("/milestones/{id}") ApiResponse<Void> deleteMilestone(@PathVariable Long projectId,@PathVariable Long id){manage(projectId);Milestone item=milestones.findById(id).filter(x->!x.deleted&&Objects.equals(x.projectId,projectId)).orElseThrow(()->service.fail(40400,HttpStatus.NOT_FOUND,"里程碑不存在"));item.deleted=true;milestones.save(item);service.log("DELETE","MILESTONE",id,item.name);return ApiResponse.ok(null);}
    private void apply(Milestone target,MilestoneInput input){target.name=input.name();target.dueDate=input.dueDate();target.status=validMilestoneStatus(input.status());}
    private String validMilestoneStatus(String status){String value=Optional.ofNullable(status).orElse("pending");if(!Set.of("pending","in_progress","completed","overdue").contains(value))throw service.fail(40001,HttpStatus.BAD_REQUEST,"里程碑状态无效");return value;}
    private Project access(Long id){Project project=service.project(id);service.ensureProjectAccess(project,service.current());return project;}
    private Project manage(Long id){Project project=service.project(id);service.ensureProjectManager(project,service.current());return project;}
    private Map<String,Object> view(Milestone item){return Map.of("id",item.id,"projectId",item.projectId,"name",item.name,"dueDate",Optional.ofNullable(item.dueDate).map(Object::toString).orElse(""),"status",item.status);}
}
