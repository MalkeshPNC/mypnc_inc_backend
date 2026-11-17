package com.pnc.masters.party;

import com.pnc.masters.configuration.SubConfigEntry;
import com.pnc.masters.configuration.SubConfigEntryRepository;
import com.pnc.masters.configuration.SubConfigType;
import com.pnc.masters.configuration.SubConfigTypeRepository;
import com.pnc.masters.party.api.PartyValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PartyCatalog implements PartyLookup {

    static final String NAME = "Name";
    static final String TYPE = "Type";

    private final SubConfigTypeRepository types;
    private final SubConfigEntryRepository entries;

    public PartyCatalog(SubConfigTypeRepository types, SubConfigEntryRepository entries) {
        this.types = types;
        this.entries = entries;
    }

    @Override
    public PartyChoice resolve(PartyList list, Long typeEntryId, Long subtypeEntryId) {
        if (typeEntryId == null) {
            throw new PartyValidationException("Type is required");
        }
        SubConfigType typeMaster = types.findByTypeCodeIgnoreCase(list.typeCode())
                .orElseThrow(() -> new PartyValidationException("Add " + list.typeLabel() + " in Masters Data first."));
        SubConfigEntry typeEntry = entries.findById(typeEntryId)
                .orElseThrow(() -> new PartyValidationException("Choose a type from the list"));
        if (typeEntry.getType() == null || !typeMaster.getTypeId().equals(typeEntry.getType().getTypeId())) {
            throw new PartyValidationException("Choose a type from the list");
        }
        String typeName = labeled(typeMaster, typeEntry, NAME, list.typeLabel());
        List<SubConfigEntry> matches = subtypesFor(list, typeName);
        if (matches.isEmpty()) {
            if (list.subtypeRequired()) {
                throw new PartyValidationException("Add a sub type for " + typeName + " in Masters Data first.");
            }
            if (subtypeEntryId != null) {
                throw new PartyValidationException("Choose a sub type that belongs to the selected type");
            }
            return new PartyChoice(typeEntryId, typeName, null, null);
        }
        if (subtypeEntryId == null) {
            throw new PartyValidationException("Sub type is required");
        }
        SubConfigEntry subtype = matches.stream()
                .filter(entry -> subtypeEntryId.equals(entry.getEntryId()))
                .findFirst()
                .orElseThrow(() -> new PartyValidationException("Choose a sub type that belongs to the selected type"));
        String subtypeName = labeled(subtype.getType(), subtype, NAME, list.subtypeLabel());
        return new PartyChoice(typeEntryId, typeName, subtype.getEntryId(), subtypeName);
    }

    @Override
    public Map<Long, String> names(Collection<Long> entryIds) {
        Set<Long> ids = new LinkedHashSet<>();
        if (entryIds != null) {
            for (Long entryId : entryIds) {
                if (entryId != null) {
                    ids.add(entryId);
                }
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (SubConfigEntry entry : entries.findAllById(ids)) {
            names.put(entry.getEntryId(), displayName(entry));
        }
        return names;
    }

    private List<SubConfigEntry> subtypesFor(PartyList list, String typeName) {
        SubConfigType subtypeMaster = types.findByTypeCodeIgnoreCase(list.subtypeCode()).orElse(null);
        if (subtypeMaster == null) {
            if (list.subtypeRequired()) {
                throw new PartyValidationException("Add " + list.subtypeLabel() + " in Masters Data first.");
            }
            return List.of();
        }
        int typeColumn = column(subtypeMaster, TYPE);
        if (typeColumn < 0) {
            throw new PartyValidationException("Add a Type column to " + list.subtypeLabel() + " in Masters Data.");
        }
        int nameColumn = column(subtypeMaster, NAME);
        if (nameColumn < 0) {
            throw new PartyValidationException("Add a Name column to " + list.subtypeLabel() + " in Masters Data.");
        }
        String wanted = typeName.trim().toLowerCase(Locale.ROOT);
        return entries.findByTypeTypeIdOrderByCreatedAtDesc(subtypeMaster.getTypeId()).stream()
                .filter(entry -> wanted.equals(text(entry.getFieldValue(typeColumn)).toLowerCase(Locale.ROOT)))
                .filter(entry -> !text(entry.getFieldValue(nameColumn)).isBlank())
                .toList();
    }

    private static String labeled(SubConfigType type, SubConfigEntry entry, String label, String masterLabel) {
        int index = column(type, label);
        if (index < 0) {
            throw new PartyValidationException("Add a " + label + " column to " + masterLabel + " in Masters Data.");
        }
        String value = text(entry.getFieldValue(index));
        if (value.isBlank()) {
            throw new PartyValidationException("Choose a type from the list");
        }
        return value;
    }

    private static String displayName(SubConfigEntry entry) {
        SubConfigType type = entry.getType();
        int index = type == null ? -1 : column(type, NAME);
        String value = text(entry.getFieldValue(index > 0 ? index : 1));
        return value.isBlank() ? "—" : value;
    }

    private static int column(SubConfigType type, String label) {
        if (type == null) {
            return -1;
        }
        String wanted = label.trim().toLowerCase(Locale.ROOT);
        for (int index = 1; index <= 8; index++) {
            String current = type.getFieldLabel(index);
            if (current != null && current.trim().toLowerCase(Locale.ROOT).equals(wanted)) {
                return index;
            }
        }
        return -1;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }
}
