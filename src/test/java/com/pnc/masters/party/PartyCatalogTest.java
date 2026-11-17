package com.pnc.masters.party;

import com.pnc.masters.configuration.SubConfigEntry;
import com.pnc.masters.configuration.SubConfigEntryRepository;
import com.pnc.masters.configuration.SubConfigType;
import com.pnc.masters.configuration.SubConfigTypeRepository;
import com.pnc.masters.party.api.PartyValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartyCatalogTest {

    @Mock private SubConfigTypeRepository types;
    @Mock private SubConfigEntryRepository entries;

    private PartyCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new PartyCatalog(types, entries);
    }

    @Test
    void rejectsATypeFromTheWrongMaster() {
        SubConfigType vendorTypes = master(1L, "vendortype", "Name");
        SubConfigType other = master(9L, "department", "Name");
        when(types.findByTypeCodeIgnoreCase("vendortype")).thenReturn(Optional.of(vendorTypes));
        when(entries.findById(40L)).thenReturn(Optional.of(entry(40L, other, "Engineering")));

        assertThatThrownBy(() -> catalog.resolve(PartyList.VENDOR, 40L, 20L))
                .isInstanceOf(PartyValidationException.class)
                .hasMessage("Choose a type from the list");
    }

    @Test
    void rejectsASubtypeWhoseTypeColumnDoesNotMatch() {
        SubConfigType vendorTypes = master(1L, "vendortype", "Name");
        SubConfigType vendorSubtypes = master(2L, "vendorsubtype", "Name", "Type");
        when(types.findByTypeCodeIgnoreCase("vendortype")).thenReturn(Optional.of(vendorTypes));
        when(types.findByTypeCodeIgnoreCase("vendorsubtype")).thenReturn(Optional.of(vendorSubtypes));
        when(entries.findById(10L)).thenReturn(Optional.of(entry(10L, vendorTypes, "PCB")));
        when(entries.findByTypeTypeIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(
                entry(20L, vendorSubtypes, "PCB Materials", "PCB"),
                entry(30L, vendorSubtypes, "ASSEMBLY: SMT", "Assembly")
        ));

        assertThatThrownBy(() -> catalog.resolve(PartyList.VENDOR, 10L, 30L))
                .isInstanceOf(PartyValidationException.class)
                .hasMessage("Choose a sub type that belongs to the selected type");
    }

    @Test
    void manufacturerCanBeSavedWithTypeOnly() {
        SubConfigType manufacturerTypes = master(3L, "manufacturertype", "Name");
        when(types.findByTypeCodeIgnoreCase("manufacturertype")).thenReturn(Optional.of(manufacturerTypes));
        when(types.findByTypeCodeIgnoreCase("manufacturersubtype")).thenReturn(Optional.empty());
        when(entries.findById(10L)).thenReturn(Optional.of(entry(10L, manufacturerTypes, "Assembly")));

        PartyChoice choice = catalog.resolve(PartyList.MANUFACTURER, 10L, null);

        assertThat(choice.typeName()).isEqualTo("Assembly");
        assertThat(choice.subtypeEntryId()).isNull();
        assertThat(choice.subtypeName()).isNull();
    }

    @Test
    void manufacturerSubtypeIsRequiredOnceRowsExist() {
        SubConfigType manufacturerTypes = master(3L, "manufacturertype", "Name");
        SubConfigType manufacturerSubtypes = master(4L, "manufacturersubtype", "Name", "Type");
        when(types.findByTypeCodeIgnoreCase("manufacturertype")).thenReturn(Optional.of(manufacturerTypes));
        when(types.findByTypeCodeIgnoreCase("manufacturersubtype")).thenReturn(Optional.of(manufacturerSubtypes));
        when(entries.findById(10L)).thenReturn(Optional.of(entry(10L, manufacturerTypes, "PCB")));
        when(entries.findByTypeTypeIdOrderByCreatedAtDesc(4L)).thenReturn(List.of(
                entry(21L, manufacturerSubtypes, "Bare board", "PCB")
        ));

        assertThatThrownBy(() -> catalog.resolve(PartyList.MANUFACTURER, 10L, null))
                .isInstanceOf(PartyValidationException.class)
                .hasMessage("Sub type is required");
    }

    private static SubConfigType master(Long id, String code, String... labels) {
        SubConfigType type = new SubConfigType();
        type.setTypeId(id);
        type.setTypeCode(code);
        for (int index = 0; index < labels.length; index++) {
            type.setFieldLabel(index + 1, labels[index]);
        }
        return type;
    }

    private static SubConfigEntry entry(Long id, SubConfigType type, String... values) {
        SubConfigEntry entry = new SubConfigEntry();
        entry.setEntryId(id);
        entry.setType(type);
        for (int index = 0; index < values.length; index++) {
            entry.setFieldValue(index + 1, values[index]);
        }
        return entry;
    }
}
