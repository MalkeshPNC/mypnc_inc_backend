package com.pnc.masters.security;

import com.pnc.masters.configuration.SubConfigEntryRepository;
import com.pnc.masters.configuration.SubConfigTypeRepository;
import com.pnc.masters.security.api.EmployeeGroupMemberResponse;
import com.pnc.masters.security.api.EmployeeGroupNotFoundException;
import com.pnc.masters.security.api.EmployeeGroupRequest;
import com.pnc.masters.security.api.EmployeeGroupResponse;
import com.pnc.masters.security.api.EmployeeGroupValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class EmployeeGroupService {

    static final String GROUP_TYPES_NAME = "Group Types";

    private final EmployeeGroupRepository groups;
    private final EmployeeGroupMemberRepository members;
    private final AppUserRepository users;
    private final SubConfigTypeRepository types;
    private final SubConfigEntryRepository entries;
    private final Clock clock;

    public EmployeeGroupService(EmployeeGroupRepository groups,
                                EmployeeGroupMemberRepository members,
                                AppUserRepository users,
                                SubConfigTypeRepository types,
                                SubConfigEntryRepository entries,
                                Clock clock) {
        this.groups = groups;
        this.members = members;
        this.users = users;
        this.types = types;
        this.entries = entries;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<EmployeeGroupResponse> list() {
        List<EmployeeGroup> rows = groups.findByDeletedFalseOrderByCreatedAtDescGroupIdDesc();
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = rows.stream().map(EmployeeGroup::getGroupId).toList();
        Map<Long, List<EmployeeGroupMemberResponse>> membersByGroup = membersByGroup(groupIds);
        Map<Long, String> names = names(rows.stream().map(EmployeeGroup::getCreatedBy).toList());
        return rows.stream()
                .map(row -> toResponse(row, names, membersByGroup.getOrDefault(row.getGroupId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> groupTypes() {
        return types.findAllByOrderByTypeNameAsc().stream()
                .filter(type -> GROUP_TYPES_NAME.equalsIgnoreCase(type.getTypeName() == null ? "" : type.getTypeName().trim()))
                .findFirst()
                .map(type -> entries.findByTypeTypeIdOrderByCreatedAtDesc(type.getTypeId()).stream()
                        .map(entry -> entry.getFieldValue(1))
                        .filter(value -> value != null && !value.isBlank())
                        .map(String::trim)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList())
                .orElse(List.of());
    }

    public EmployeeGroupResponse create(Long userId, EmployeeGroupRequest request) {
        EmployeeGroup group = new EmployeeGroup();
        group.setName(requireName(request));
        group.setType(requireType(request));
        group.setCreatedBy(userId);
        group.setCreatedAt(LocalDateTime.now(clock));
        group.setDeleted(false);
        group = groups.save(group);
        List<EmployeeGroupMemberResponse> people = replaceMembers(group.getGroupId(), request == null ? null : request.userIds());
        return toResponse(group, names(List.of(userId)), people);
    }

    public EmployeeGroupResponse update(Long groupId, EmployeeGroupRequest request) {
        EmployeeGroup group = requireActive(groupId);
        group.setName(requireName(request));
        group.setType(requireType(request));
        group = groups.save(group);
        List<EmployeeGroupMemberResponse> people = replaceMembers(group.getGroupId(), request.userIds());
        return toResponse(group, names(List.of(group.getCreatedBy())), people);
    }

    public void delete(Long groupId) {
        EmployeeGroup group = requireActive(groupId);
        members.deleteByGroupId(groupId);
        group.setDeleted(true);
        groups.save(group);
    }

    private EmployeeGroup requireActive(Long groupId) {
        EmployeeGroup group = groups.findById(groupId).orElseThrow(() -> new EmployeeGroupNotFoundException(groupId));
        if (group.isDeleted()) {
            throw new EmployeeGroupNotFoundException(groupId);
        }
        return group;
    }

    private List<EmployeeGroupMemberResponse> replaceMembers(Long groupId, List<Long> userIds) {
        List<Long> ids = distinct(userIds);
        Map<Long, String> names = requireEnabled(ids);
        members.deleteByGroupId(groupId);
        members.flush();
        List<EmployeeGroupMemberResponse> people = new ArrayList<>();
        for (Long userId : ids) {
            EmployeeGroupMember row = new EmployeeGroupMember();
            row.setGroupId(groupId);
            row.setUserId(userId);
            members.save(row);
            people.add(new EmployeeGroupMemberResponse(userId, names.get(userId)));
        }
        return people;
    }

    private Map<Long, String> requireEnabled(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (AppUser user : users.findAllById(userIds)) {
            if (user.isEnabled()) {
                names.put(user.getUserId(), display(user));
            }
        }
        if (names.size() != userIds.size()) {
            throw new EmployeeGroupValidationException("Choose a person from the list");
        }
        return names;
    }

    private String requireName(EmployeeGroupRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new EmployeeGroupValidationException("Name is required");
        }
        String name = request.name().trim();
        if (name.length() > 200) {
            throw new EmployeeGroupValidationException("Name must be 200 characters or less");
        }
        return name;
    }

    private String requireType(EmployeeGroupRequest request) {
        if (request == null || request.type() == null || request.type().isBlank()) {
            throw new EmployeeGroupValidationException("Type is required");
        }
        String type = request.type().trim();
        List<String> allowed = groupTypes();
        if (allowed.isEmpty()) {
            throw new EmployeeGroupValidationException("Add Group Types in Masters Data first.");
        }
        if (!allowed.contains(type)) {
            throw new EmployeeGroupValidationException("Choose a Group Type from the list");
        }
        return type;
    }

    private Map<Long, List<EmployeeGroupMemberResponse>> membersByGroup(Collection<Long> groupIds) {
        List<EmployeeGroupMember> links = members.findByGroupIdIn(groupIds);
        Map<Long, String> names = names(links.stream().map(EmployeeGroupMember::getUserId).toList());
        Map<Long, List<EmployeeGroupMemberResponse>> grouped = new HashMap<>();
        for (EmployeeGroupMember link : links) {
            grouped.computeIfAbsent(link.getGroupId(), key -> new ArrayList<>())
                    .add(new EmployeeGroupMemberResponse(link.getUserId(), names.getOrDefault(link.getUserId(), "Unknown")));
        }
        return grouped;
    }

    private Map<Long, String> names(Collection<Long> userIds) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Long userId : userIds) {
            if (userId != null) {
                ids.add(userId);
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        users.findAllById(ids).forEach(user -> names.put(user.getUserId(), display(user)));
        return names;
    }

    private EmployeeGroupResponse toResponse(EmployeeGroup group,
                                             Map<Long, String> names,
                                             List<EmployeeGroupMemberResponse> people) {
        return new EmployeeGroupResponse(
                group.getGroupId(),
                group.getName(),
                group.getType(),
                group.getCreatedBy(),
                names.getOrDefault(group.getCreatedBy(), "Unknown"),
                group.getCreatedAt(),
                people);
    }

    private static String display(AppUser user) {
        String name = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        if (!name.isEmpty()) {
            return name;
        }
        return user.getEmail() == null ? "Unknown" : user.getEmail();
    }

    private static List<Long> distinct(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userIds.stream().filter(id -> id != null).distinct().toList();
    }
}
