package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.SubConfigurationEntryRequest;
import com.pnc.masters.configuration.api.SubConfigurationTypeExistsException;
import com.pnc.masters.configuration.api.SubConfigurationTypeRequest;
import com.pnc.masters.configuration.api.SubConfigurationValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubConfigurationServiceTest {

    @Mock private SubConfigTypeRepository typeRepository;
    @Mock private SubConfigEntryRepository entryRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks
    private SubConfigurationService service;

    @Test
    void createTypeStoresOnlyGivenLabels() {
        AppUser user = new AppUser();
        user.setUserId(7L);
        user.setDisplayName("Ada Lovelace");
        when(typeRepository.existsByTypeCodeIgnoreCase("department")).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(typeRepository.save(any(SubConfigType.class))).thenAnswer(invocation -> {
            SubConfigType saved = invocation.getArgument(0);
            saved.setTypeId(1L);
            return saved;
        });

        var response = service.createType(new SubConfigurationTypeRequest(
                "Department",
                "Department",
                List.of("Name", "Code")
        ), 7L);

        assertThat(response.typeCode()).isEqualTo("department");
        assertThat(response.fieldLabels()).containsExactly("Name", "Code");
        assertThat(response.entries()).isEmpty();
        ArgumentCaptor<SubConfigType> captor = ArgumentCaptor.forClass(SubConfigType.class);
        verify(typeRepository).save(captor.capture());
        assertThat(captor.getValue().getFieldLabel(1)).isEqualTo("Name");
        assertThat(captor.getValue().getFieldLabel(2)).isEqualTo("Code");
        assertThat(captor.getValue().getFieldLabel(3)).isNull();
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("Ada Lovelace");
    }

    @Test
    void createTypeRejectsDuplicateCode() {
        when(typeRepository.existsByTypeCodeIgnoreCase("department")).thenReturn(true);

        assertThatThrownBy(() -> service.createType(
                new SubConfigurationTypeRequest("department", "Department", List.of("Name")),
                1L
        )).isInstanceOf(SubConfigurationTypeExistsException.class);
    }

    @Test
    void createEntryWritesAlignedValues() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        type.setTypeCode("department");
        type.setFieldLabel(1, "Name");
        type.setFieldLabel(2, "Code");
        AppUser user = new AppUser();
        user.setUserId(7L);
        user.setDisplayName("Ada Lovelace");
        when(typeRepository.findById(3L)).thenReturn(Optional.of(type));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(entryRepository.save(any(SubConfigEntry.class))).thenAnswer(invocation -> {
            SubConfigEntry saved = invocation.getArgument(0);
            saved.setEntryId(11L);
            return saved;
        });

        var response = service.createEntry(3L, new SubConfigurationEntryRequest(List.of("Engineering", "ENG")), 7L);

        assertThat(response.values()).containsExactly("Engineering", "ENG");
        ArgumentCaptor<SubConfigEntry> captor = ArgumentCaptor.forClass(SubConfigEntry.class);
        verify(entryRepository).save(captor.capture());
        assertThat(captor.getValue().getFieldValue(1)).isEqualTo("Engineering");
        assertThat(captor.getValue().getFieldValue(2)).isEqualTo("ENG");
        assertThat(captor.getValue().getFieldValue(3)).isNull();
    }

    @Test
    void updateTypeKeepsTheCodeAndAddsAColumn() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        type.setTypeCode("group-types");
        type.setTypeName("Group Types");
        type.setFieldLabel(1, "Name");
        SubConfigEntry entry = new SubConfigEntry();
        entry.setEntryId(11L);
        entry.setType(type);
        entry.setFieldValue(1, "Shift");
        entry.setCreatedBy("Ada");
        when(typeRepository.findById(3L)).thenReturn(Optional.of(type));
        when(typeRepository.save(any(SubConfigType.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(entryRepository.findByTypeTypeIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(entry));

        var response = service.updateType(3L, new SubConfigurationTypeRequest(
                "ignored",
                "Group Types",
                List.of("Name", "Code")
        ));

        assertThat(response.typeCode()).isEqualTo("group-types");
        assertThat(response.fieldLabels()).containsExactly("Name", "Code");
        assertThat(response.entries().getFirst().values()).containsExactly("Shift", null);
    }

    @Test
    void updateTypeKeepsExistingColumns() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        type.setFieldLabel(1, "Name");
        type.setFieldLabel(2, "Code");
        when(typeRepository.findById(3L)).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> service.updateType(3L, new SubConfigurationTypeRequest(
                "group-types",
                "Group Types",
                List.of("Name")
        ))).isInstanceOf(SubConfigurationValidationException.class)
                .hasMessage("Keep the columns that already exist. You can add a new one.");
    }

    @Test
    void updateEntryReplacesValues() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        type.setFieldLabel(1, "Name");
        SubConfigEntry entry = new SubConfigEntry();
        entry.setEntryId(11L);
        entry.setType(type);
        entry.setFieldValue(1, "Shift");
        entry.setCreatedBy("Ada");
        when(typeRepository.findById(3L)).thenReturn(Optional.of(type));
        when(entryRepository.findById(11L)).thenReturn(Optional.of(entry));
        when(entryRepository.save(any(SubConfigEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateEntry(3L, 11L, new SubConfigurationEntryRequest(List.of("Line")));

        assertThat(response.values()).containsExactly("Line");
        assertThat(entry.getCreatedBy()).isEqualTo("Ada");
    }

    @Test
    void createEntryRejectsWrongValueCount() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        type.setFieldLabel(1, "Name");
        type.setFieldLabel(2, "Code");
        when(typeRepository.findById(3L)).thenReturn(Optional.of(type));

        assertThatThrownBy(() -> service.createEntry(
                3L,
                new SubConfigurationEntryRequest(List.of("Engineering")),
                7L
        )).isInstanceOf(SubConfigurationValidationException.class);
    }

    @Test
    void deleteEntryRemovesRow() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        SubConfigEntry entry = new SubConfigEntry();
        entry.setEntryId(11L);
        entry.setType(type);
        when(entryRepository.findById(11L)).thenReturn(Optional.of(entry));

        service.deleteEntry(3L, 11L);

        verify(entryRepository).delete(entry);
    }
}
