package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.SubConfigurationEntryRequest;
import com.pnc.masters.configuration.api.SubConfigurationTypeExistsException;
import com.pnc.masters.configuration.api.SubConfigurationTypeRequest;
import com.pnc.masters.configuration.api.SubConfigurationTypeResponse;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubConfigurationServiceTest {

    @Mock private SubConfigTypeRepository typeRepository;
    @Mock private SubConfigEntryRepository entryRepository;
    @Mock private AppUserRepository userRepository;
    @Mock private MasterEntryUsage entryUsage;

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
                List.of("Name", "Code"),
                "quote"
        ), 7L);

        assertThat(response.typeCode()).isEqualTo("department");
        assertThat(response.moduleCode()).isEqualTo("QUOTE");
        assertThat(response.fieldLabels()).containsExactly("Name", "Code");
        assertThat(response.entries()).isEmpty();
        ArgumentCaptor<SubConfigType> captor = ArgumentCaptor.forClass(SubConfigType.class);
        verify(typeRepository).save(captor.capture());
        assertThat(captor.getValue().getFieldLabel(1)).isEqualTo("Name");
        assertThat(captor.getValue().getFieldLabel(2)).isEqualTo("Code");
        assertThat(captor.getValue().getFieldLabel(3)).isNull();
        assertThat(captor.getValue().getModuleCode()).isEqualTo("QUOTE");
        assertThat(captor.getValue().getCreatedBy()).isEqualTo("Ada Lovelace");
    }

    @Test
    void createTypeRejectsDuplicateCode() {
        when(typeRepository.existsByTypeCodeIgnoreCase("department")).thenReturn(true);

        assertThatThrownBy(() -> service.createType(
                new SubConfigurationTypeRequest("department", "Department", List.of("Name"), null),
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
        when(userRepository.findById(7L)).thenReturn(Optional.of(editor()));

        var response = service.updateType(3L, new SubConfigurationTypeRequest(
                "ignored",
                "Group Types",
                List.of("Name", "Code"),
                null
        ), 7L);

        assertThat(response.typeCode()).isEqualTo("group-types");
        assertThat(response.fieldLabels()).containsExactly("Name", "Code");
        assertThat(response.entries().getFirst().values()).containsExactly("Shift", null);
        assertThat(response.updatedBy()).isEqualTo("Grace");
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
                List.of("Name"),
                null
        ), 7L)).isInstanceOf(SubConfigurationValidationException.class)
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
        when(userRepository.findById(7L)).thenReturn(Optional.of(editor()));

        var response = service.updateEntry(3L, 11L, new SubConfigurationEntryRequest(List.of("Line")), 7L);

        assertThat(response.values()).containsExactly("Line");
        assertThat(entry.getCreatedBy()).isEqualTo("Ada");
        assertThat(entry.getUpdatedBy()).isEqualTo("Grace");
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

    @Test
    void deleteEntryRejectsARowUsedByAVendorOrManufacturer() {
        SubConfigType type = new SubConfigType();
        type.setTypeId(3L);
        SubConfigEntry entry = new SubConfigEntry();
        entry.setEntryId(11L);
        entry.setType(type);
        when(entryRepository.findById(11L)).thenReturn(Optional.of(entry));
        when(entryUsage.isReferenced(11L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEntry(3L, 11L))
                .isInstanceOf(SubConfigurationValidationException.class)
                .hasMessage("This row is used by a vendor or manufacturer and cannot be removed.");
    }

    @Test
    void findTypesKeepsSharedListsAndTheRequestedModule() {
        SubConfigType shared = new SubConfigType();
        shared.setTypeId(1L);
        shared.setTypeCode("shared");
        shared.setTypeName("Shared");
        SubConfigType quote = new SubConfigType();
        quote.setTypeId(2L);
        quote.setTypeCode("quotetypes");
        quote.setTypeName("Quote Types");
        quote.setModuleCode("QUOTE");
        when(typeRepository.findSharedOrModule("QUOTE")).thenReturn(List.of(shared, quote));
        when(entryRepository.findByTypeTypeIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        var types = service.findTypes("QUOTE");

        assertThat(types).extracting(SubConfigurationTypeResponse::typeCode)
                .containsExactly("shared", "quotetypes");
        assertThat(types.get(1).moduleCode()).isEqualTo("QUOTE");
        verify(typeRepository, never()).findAllByOrderByTypeNameAsc();
    }

    private static AppUser editor() {
        AppUser user = new AppUser();
        user.setUserId(7L);
        user.setDisplayName("Grace");
        return user;
    }
}
