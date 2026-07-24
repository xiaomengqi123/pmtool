package com.pmtool;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;

record DepartmentInput(String name, Long parentId) {}

@RestController
@RequestMapping("/api/v1")
class OrganizationController {
    private final PmToolService service; private final DepartmentRepository departments; private final RoleRepository roles;
    OrganizationController(PmToolService service,DepartmentRepository departments,RoleRepository roles){this.service=service;this.departments=departments;this.roles=roles;}
    @GetMapping("/departments") ApiResponse<?> departments(){service.requireAdmin();return ApiResponse.ok(departments.findByDeletedFalseOrderByNameAsc().stream().map(this::departmentView).toList());}
    @PostMapping("/departments") ApiResponse<?> createDepartment(@RequestBody DepartmentInput input){service.requireAdmin();validate(input);Department d=new Department();d.name=input.name();d.parentId=input.parentId();departments.save(d);service.log("CREATE","DEPARTMENT",d.id,d.name);return ApiResponse.ok(departmentView(d));}
    @PutMapping("/departments/{id}") ApiResponse<?> updateDepartment(@PathVariable Long id,@RequestBody DepartmentInput input){service.requireAdmin();validate(input);Department d=department(id);if(Objects.equals(id,input.parentId()))throw service.fail(40001,HttpStatus.BAD_REQUEST,"部门不能设置自身为上级");d.name=input.name();d.parentId=input.parentId();departments.save(d);service.log("UPDATE","DEPARTMENT",id,d.name);return ApiResponse.ok(departmentView(d));}
    @DeleteMapping("/departments/{id}") ApiResponse<Void> deleteDepartment(@PathVariable Long id){service.requireAdmin();Department d=department(id);if(departments.findByDeletedFalseOrderByNameAsc().stream().anyMatch(x->Objects.equals(x.parentId,id)))throw service.fail(40001,HttpStatus.BAD_REQUEST,"请先处理下级部门");d.deleted=true;departments.save(d);service.log("DELETE","DEPARTMENT",id,d.name);return ApiResponse.ok(null);}
    @GetMapping("/roles") ApiResponse<?> roles(){service.requireAdmin();return ApiResponse.ok(roles.findAllByOrderByIdAsc().stream().map(r->Map.of("id",r.id,"code",r.code,"name",r.name,"description",Optional.ofNullable(r.description).orElse(""))).toList());}
    private Department department(Long id){return departments.findById(id).filter(x->!x.deleted).orElseThrow(()->service.fail(40400,HttpStatus.NOT_FOUND,"部门不存在"));}
    private void validate(DepartmentInput input){if(input.name()==null||input.name().isBlank())throw service.fail(40001,HttpStatus.BAD_REQUEST,"部门名称不能为空");if(input.parentId()!=null)department(input.parentId());}
    private Map<String,Object> departmentView(Department d){return Map.of("id",d.id,"name",d.name,"parentId",Optional.ofNullable(d.parentId).orElse(0L));}
}
