package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.SubConfigurationEntryNotFoundException;
import com.pnc.masters.configuration.api.SubConfigurationEntryRequest;
import com.pnc.masters.configuration.api.SubConfigurationEntryResponse;
import com.pnc.masters.configuration.api.SubConfigurationTypeExistsException;
import com.pnc.masters.configuration.api.SubConfigurationTypeNotFoundException;
import com.pnc.masters.configuration.api.SubConfigurationTypeRequest;
import com.pnc.masters.configuration.api.SubConfigurationTypeResponse;
import com.pnc.masters.configuration.api.SubConfigurationValidationException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SubConfigurationService {

    private final SubConfigTypeRepository typeRepository;
    private final SubConfigEntryRepository entryRepository;
    private final AppUserRepository userRepository;

    public SubConfigurationService(
            SubConfigTypeRepository typeRepository,
            SubConfigEntryRepository entryRepository,
            AppUserRepository userRepository
    ) {
        this.typeRepository = typeRepository;
        this.entryRepository = entryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SubConfigurationTypeResponse> findAllTypes() {
        return typeRepository.findAllByOrderByTypeNameAsc().stream()
                .map(this::toTypeResponse)
                .toList();
    }

    public SubConfigurationTypeResponse createType(SubConfigurationTypeRequest request, Long userId) {
        String typeCode = request.typeCode().trim().toLowerCase(Locale.ROOT);
        if (typeRepository.existsByTypeCodeIgnoreCase(typeCode)) {
            throw new SubConfigurationTypeExistsException(typeCode);
        }
        List<String> labels = normalizeLabels(request.fieldLabels());
        SubConfigType type = new SubConfigType();
        type.setTypeCode(typeCode);
        type.setTypeName(request.typeName().trim());
        SubConfigSlots.applyLabels(type, labels);
        applyCreator(type, userId);
        SubConfigType saved = typeRepository.save(type);
        return new SubConfigurationTypeResponse(
                saved.getTypeId(),
                saved.getTypeCode(),
                saved.getTypeName(),
                SubConfigSlots.labelsOf(saved),
                List.of()
        );
    }

    public SubConfigurationEntryResponse createEntry(Long typeId, SubConfigurationEntryRequest request, Long userId) {
        SubConfigType type = typeRepository.findById(typeId)
                .orElseThrow(() -> new SubConfigurationTypeNotFoundException(typeId));
        List<String> labels = SubConfigSlots.labelsOf(type);
        List<String> values = normalizeValues(request.values(), labels.size());
        SubConfigEntry entry = new SubConfigEntry();
        entry.setType(type);
        SubConfigSlots.applyValues(entry, values);
        applyCreator(entry, userId);
        return toEntryResponse(entryRepository.save(entry), labels.size());
    }

    public void deleteEntry(Long typeId, Long entryId) {
        SubConfigEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new SubConfigurationEntryNotFoundException(entryId));
        if (!entry.getType().getTypeId().equals(typeId)) {
            throw new SubConfigurationEntryNotFoundException(entryId);
        }
        entryRepository.delete(entry);
    }

    private SubConfigurationTypeResponse toTypeResponse(SubConfigType type) {
        List<String> labels = SubConfigSlots.labelsOf(type);
        List<SubConfigurationEntryResponse> entries = entryRepository
                .findByTypeTypeIdOrderByCreatedAtDesc(type.getTypeId())
                .stream()
                .map(entry -> toEntryResponse(entry, labels.size()))
                .toList();
        return new SubConfigurationTypeResponse(
                type.getTypeId(),
                type.getTypeCode(),
                type.getTypeName(),
                labels,
                entries
        );
    }

    private static SubConfigurationEntryResponse toEntryResponse(SubConfigEntry entry, int fieldCount) {
        return new SubConfigurationEntryResponse(
                entry.getEntryId(),
                SubConfigSlots.valuesOf(entry, fieldCount),
                entry.getCreatedBy(),
                entry.getCreatedAt()
        );
    }

    private void applyCreator(SubConfigType type, Long userId) {
        AppUser user = userRepository.findById(userId).orElse(null);
        type.setCreatedBy(user == null ? "Unknown" : user.getDisplayName());
    }

    private void applyCreator(SubConfigEntry entry, Long userId) {
        AppUser user = userRepository.findById(userId).orElse(null);
        entry.setCreatedByUserId(user == null ? null : user.getUserId());
        entry.setCreatedBy(user == null ? "Unknown" : user.getDisplayName());
    }

    private static List<String> normalizeLabels(List<String> fieldLabels) {
        List<String> labels = new ArrayList<>();
        for (String label : fieldLabels) {
            if (label == null || label.isBlank()) {
                continue;
            }
            labels.add(label.trim());
        }
        if (labels.isEmpty() || labels.size() > SubConfigSlots.MAX) {
            throw new SubConfigurationValidationException("Provide between 1 and 8 field labels");
        }
        return labels;
    }

    private static List<String> normalizeValues(List<String> values, int fieldCount) {
        if (values == null || values.size() != fieldCount) {
            throw new SubConfigurationValidationException("Provide exactly " + fieldCount + " values");
        }
        List<String> normalized = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                normalized.add(null);
            } else {
                normalized.add(value.trim());
            }
        }
        return normalized;
    }
}
