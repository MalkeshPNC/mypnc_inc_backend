package com.pnc.masters.security;

import com.pnc.masters.security.api.EmployeeGroupRequest;
import com.pnc.masters.security.api.EmployeeGroupResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-groups")
public class EmployeeGroupController {

    private final EmployeeGroupService groups;

    public EmployeeGroupController(EmployeeGroupService groups) {
        this.groups = groups;
    }

    @GetMapping
    public List<EmployeeGroupResponse> list() {
        return groups.list();
    }

    @GetMapping("/group-types")
    public List<String> groupTypes() {
        return groups.groupTypes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeGroupResponse create(@RequestBody EmployeeGroupRequest request, Authentication authentication) {
        return groups.create(userId(authentication), request);
    }

    @PutMapping("/{id}")
    public EmployeeGroupResponse update(@PathVariable Long id, @RequestBody EmployeeGroupRequest request) {
        return groups.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        groups.delete(id);
    }

    private static Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
