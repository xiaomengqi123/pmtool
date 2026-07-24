package com.pmtool;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

record CustomerContactInput(String name, String positionName, String phone, String email) {}
record CustomerFollowUpInput(String content, LocalDateTime followUpAt) {}

@RestController
@RequestMapping("/api/v1/customers/{customerId}")
class CustomerDetailsController {
    private final PmToolService service;
    private final CustomerContactRepository contacts;
    private final CustomerFollowUpRepository followUps;

    CustomerDetailsController(PmToolService service, CustomerContactRepository contacts, CustomerFollowUpRepository followUps) {
        this.service=service; this.contacts=contacts; this.followUps=followUps;
    }

    @GetMapping("/contacts") ApiResponse<?> contacts(@PathVariable Long customerId) {
        service.customer(customerId);
        return ApiResponse.ok(contacts.findByCustomerIdAndDeletedFalse(customerId).stream().map(this::contactView).toList());
    }

    @PostMapping("/contacts") ApiResponse<?> createContact(@PathVariable Long customerId, @RequestBody CustomerContactInput input) {
        service.requireManager(); service.customer(customerId);
        if (input.name()==null || input.name().isBlank()) throw service.fail(40001, org.springframework.http.HttpStatus.BAD_REQUEST, "联系人姓名不能为空");
        CustomerContact contact=new CustomerContact(); contact.customerId=customerId; apply(contact,input); contacts.save(contact);
        service.log("CREATE","CUSTOMER_CONTACT",contact.id,contact.name);
        return ApiResponse.ok(contactView(contact));
    }

    @PutMapping("/contacts/{id}") ApiResponse<?> updateContact(@PathVariable Long customerId,@PathVariable Long id,@RequestBody CustomerContactInput input) {
        service.requireManager();
        CustomerContact contact=contacts.findById(id).filter(x->!x.deleted&&Objects.equals(x.customerId,customerId)).orElseThrow(()->service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"联系人不存在"));
        apply(contact,input); contacts.save(contact); service.log("UPDATE","CUSTOMER_CONTACT",contact.id,contact.name);
        return ApiResponse.ok(contactView(contact));
    }

    @DeleteMapping("/contacts/{id}") ApiResponse<Void> deleteContact(@PathVariable Long customerId,@PathVariable Long id) {
        service.requireManager(); CustomerContact contact=contacts.findById(id).filter(x->!x.deleted&&Objects.equals(x.customerId,customerId)).orElseThrow(()->service.fail(40400,org.springframework.http.HttpStatus.NOT_FOUND,"联系人不存在"));
        contact.deleted=true; contacts.save(contact); service.log("DELETE","CUSTOMER_CONTACT",id,contact.name); return ApiResponse.ok(null);
    }

    @GetMapping("/follow-ups") ApiResponse<?> followUps(@PathVariable Long customerId) {
        service.customer(customerId);
        return ApiResponse.ok(followUps.findByCustomerIdAndDeletedFalseOrderByFollowUpAtDesc(customerId).stream().map(this::followUpView).toList());
    }

    @PostMapping("/follow-ups") ApiResponse<?> createFollowUp(@PathVariable Long customerId,@RequestBody CustomerFollowUpInput input) {
        service.requireManager(); service.customer(customerId);
        if (input.content()==null || input.content().isBlank() || input.followUpAt()==null) throw service.fail(40001,org.springframework.http.HttpStatus.BAD_REQUEST,"跟进内容和时间不能为空");
        CustomerFollowUp item=new CustomerFollowUp(); item.customerId=customerId; item.content=input.content(); item.followUpAt=input.followUpAt(); item.creatorId=service.current().id(); followUps.save(item);
        service.log("CREATE","CUSTOMER_FOLLOW_UP",item.id,"客户跟进"); return ApiResponse.ok(followUpView(item));
    }

    private void apply(CustomerContact target, CustomerContactInput input) { target.name=input.name(); target.positionName=input.positionName(); target.phone=input.phone(); target.email=input.email(); }
    private Map<String,Object> contactView(CustomerContact c) { return Map.of("id",c.id,"customerId",c.customerId,"name",c.name,"positionName",Optional.ofNullable(c.positionName).orElse(""),"phone",Optional.ofNullable(c.phone).orElse(""),"email",Optional.ofNullable(c.email).orElse("")); }
    private Map<String,Object> followUpView(CustomerFollowUp f) { return Map.of("id",f.id,"customerId",f.customerId,"content",f.content,"followUpAt",f.followUpAt,"creatorId",f.creatorId,"createdAt",f.createdAt); }
}
