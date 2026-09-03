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
