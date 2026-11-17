package com.pnc.masters.party;

import com.pnc.masters.party.api.PartyCodeExistsException;
import com.pnc.masters.party.api.PartyInput;
import com.pnc.masters.party.api.PartyNotFoundException;
import com.pnc.masters.party.api.PartyResponse;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Transactional
public abstract class PartyService<E extends PartyRow> {

    private final PartyLookup catalog;
    private final AppUserRepository users;
    private final Clock clock;

    protected PartyService(PartyLookup catalog, AppUserRepository users, Clock clock) {
        this.catalog = catalog;
        this.users = users;
        this.clock = clock;
    }

    protected abstract PartyList listKind();

    protected abstract String missingMessage();

    protected abstract E newRow();

    protected abstract List<E> findActive();

    protected abstract Optional<E> findRow(Long id);

    protected abstract boolean codeTaken(String code, Long excludeId);

    protected abstract E save(E row);

    @Transactional(readOnly = true)
    public List<PartyResponse> list() {
        List<E> rows = findActive();
        Map<Long, String> entryNames = names(rows);
        Map<Long, String> people = userNames(rows);
        return rows.stream().map(row -> toResponse(row, entryNames, people)).toList();
    }

    @Transactional(readOnly = true)
    public PartyResponse findById(Long id) {
        E row = requireActive(id);
        return toResponse(row, names(List.of(row)), userNames(List.of(row)));
    }

    public PartyResponse create(Long userId, PartyInput request) {
        PartyFields.Values values = PartyFields.parse(request);
        requireUnique(values.code(), null);
        PartyChoice choice = catalog.resolve(listKind(), request.typeEntryId(), request.subtypeEntryId());
        E row = newRow();
        apply(row, values, choice);
        LocalDateTime now = LocalDateTime.now(clock);
        row.setCreatedBy(userId);
        row.setCreatedDate(now);
        row.setUpdatedBy(userId);
        row.setUpdatedDate(now);
        row.setDeleted(false);
        row = save(row);
        return toResponse(row, names(List.of(row)), userNames(List.of(row)));
    }

    public PartyResponse update(Long id, Long userId, PartyInput request) {
        E row = requireActive(id);
        PartyFields.Values values = PartyFields.parse(request);
        requireUnique(values.code(), row.getId());
        PartyChoice choice = catalog.resolve(listKind(), request.typeEntryId(), request.subtypeEntryId());
        apply(row, values, choice);
        row.setUpdatedBy(userId);
        row.setUpdatedDate(LocalDateTime.now(clock));
        row = save(row);
        return toResponse(row, names(List.of(row)), userNames(List.of(row)));
    }

    public void delete(Long id) {
        E row = requireActive(id);
        row.setDeleted(true);
        save(row);
    }

    private void requireUnique(String code, Long excludeId) {
        if (codeTaken(code, excludeId)) {
            throw new PartyCodeExistsException("That code is already in use.");
        }
    }

    private E requireActive(Long id) {
        E row = findRow(id).orElseThrow(() -> new PartyNotFoundException(missingMessage()));
        if (row.isDeleted()) {
            throw new PartyNotFoundException(missingMessage());
        }
        return row;
    }

    private static void apply(PartyRow row, PartyFields.Values values, PartyChoice choice) {
        row.setCode(values.code());
        row.setName(values.name());
        row.setTypeEntryId(choice.typeEntryId());
        row.setSubtypeEntryId(choice.subtypeEntryId());
        row.setContactName(values.contactName());
        row.setContactAddress(values.contactAddress());
        row.setContactEmail(values.contactEmail());
        row.setContactPhone(values.contactPhone());
    }

    private Map<Long, String> names(Collection<E> rows) {
        List<Long> ids = new ArrayList<>();
        for (E row : rows) {
            ids.add(row.getTypeEntryId());
            ids.add(row.getSubtypeEntryId());
        }
        return catalog.names(ids);
    }

    private Map<Long, String> userNames(Collection<E> rows) {
        Set<Long> ids = new LinkedHashSet<>();
        for (E row : rows) {
            if (row.getCreatedBy() != null) {
                ids.add(row.getCreatedBy());
            }
            if (row.getUpdatedBy() != null) {
                ids.add(row.getUpdatedBy());
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        users.findAllById(ids).forEach(user -> names.put(user.getUserId(), display(user)));
        return names;
    }

    private PartyResponse toResponse(E row, Map<Long, String> entryNames, Map<Long, String> userNames) {
        return new PartyResponse(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getTypeEntryId(),
                entryNames.getOrDefault(row.getTypeEntryId(), "—"),
                row.getSubtypeEntryId(),
                row.getSubtypeEntryId() == null ? null : entryNames.getOrDefault(row.getSubtypeEntryId(), "—"),
                row.getContactName(),
                row.getContactAddress(),
                row.getContactEmail(),
                row.getContactPhone(),
                row.getCreatedBy(),
                userNames.getOrDefault(row.getCreatedBy(), "Unknown"),
                row.getCreatedDate(),
                row.getUpdatedBy(),
                userNames.getOrDefault(row.getUpdatedBy(), "Unknown"),
                row.getUpdatedDate()
        );
    }

    private static String display(AppUser user) {
        String name = user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        if (!name.isEmpty()) {
            return name;
        }
        return user.getEmail() == null ? "Unknown" : user.getEmail();
    }
}
