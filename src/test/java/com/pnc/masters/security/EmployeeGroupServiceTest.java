package com.pnc.masters.security;

import com.pnc.masters.configuration.SubConfigEntry;
import com.pnc.masters.configuration.SubConfigEntryRepository;
import com.pnc.masters.configuration.SubConfigType;
import com.pnc.masters.configuration.SubConfigTypeRepository;
import com.pnc.masters.security.api.EmployeeGroupRequest;
import com.pnc.masters.security.api.EmployeeGroupValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeGroupServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    @Mock private EmployeeGroupRepository groups;
    @Mock private EmployeeGroupMemberRepository members;
    @Mock private AppUserRepository users;
    @Mock private SubConfigTypeRepository types;
    @Mock private SubConfigEntryRepository entries;

    private EmployeeGroupService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeGroupService(
                groups, members, users, types, entries, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void groupTypesComeFromTheGroupTypesMaster() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(4L);
        type.setTypeName("group types");
        SubConfigEntry first = new SubConfigEntry();
        first.setFieldValue(1, "Assembly");
        SubConfigEntry blank = new SubConfigEntry();
        blank.setFieldValue(1, "  ");
        when(types.findAllByOrderByTypeNameAsc()).thenReturn(List.of(type));
        when(entries.findByTypeTypeIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(first, blank));

        assertThat(service.groupTypes()).containsExactly("Assembly");
    }

    @Test
    void createRejectsATypeWhenTheMasterIsMissing() {
        when(types.findAllByOrderByTypeNameAsc()).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(3L, new EmployeeGroupRequest("Line 1", "Assembly", List.of())))
                .isInstanceOf(EmployeeGroupValidationException.class)
                .hasMessage("Add Group Types in Masters Data first.");
    }

    @Test
    void createStoresTheGroupAndItsMembers() {
        stubTypes("Assembly");
        when(users.findAllById(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            return ids.stream().map(id -> user(id, id == 9L ? "Sam" : "Alex")).toList();
        });
        when(groups.save(any(EmployeeGroup.class))).thenAnswer(invocation -> {
            EmployeeGroup group = invocation.getArgument(0);
            group.setGroupId(7L);
            return group;
        });

        var response = service.create(3L, new EmployeeGroupRequest("Line 1", "Assembly", List.of(9L)));

        assertThat(response.groupId()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Line 1");
        assertThat(response.type()).isEqualTo("Assembly");
        assertThat(response.createdByUserId()).isEqualTo(3L);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 10, 26, 10, 0));
        assertThat(response.members()).extracting(member -> member.displayName()).containsExactly("Sam");
        verify(members).deleteByGroupId(7L);
    }

    @Test
    void deleteHidesTheGroupAndClearsItsMembers() {
        EmployeeGroup group = new EmployeeGroup();
        group.setGroupId(7L);
        group.setDeleted(false);
        when(groups.findById(7L)).thenReturn(Optional.of(group));

        service.delete(7L);

        assertThat(group.isDeleted()).isTrue();
        verify(members).deleteByGroupId(7L);
        verify(groups).save(group);
    }

    private void stubTypes(String label) {
        SubConfigType type = new SubConfigType();
        type.setTypeId(4L);
        type.setTypeName("Group Types");
        SubConfigEntry entry = new SubConfigEntry();
        entry.setFieldValue(1, label);
        when(types.findAllByOrderByTypeNameAsc()).thenReturn(List.of(type));
        when(entries.findByTypeTypeIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(entry));
    }

    private static AppUser user(Long id, String name) {
        AppUser user = new AppUser();
        user.setUserId(id);
        user.setDisplayName(name);
        user.setEmail(name.toLowerCase() + "@example.com");
        user.setEnabled(true);
        return user;
    }
}
