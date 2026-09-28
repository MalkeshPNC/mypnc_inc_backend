package com.pnc.masters.ncmaster;

import com.pnc.masters.ncmaster.api.NcMasterNotFoundException;
import com.pnc.masters.ncmaster.api.NcMasterRequest;
import com.pnc.masters.ncmaster.api.NcMasterResponse;
import com.pnc.masters.ncmaster.api.NcMasterValidationException;
import com.pnc.masters.ncmaster.api.NcNumberAvailabilityResponse;
import com.pnc.masters.ncmaster.api.NcNumberExistsException;
import com.pnc.masters.ncmaster.api.PcbaNoteLookupResponse;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional
public class NcMasterService {

    private final NcMasterRepository ncMasterRepository;
    private final PcbaNoteRepository pcbaNoteRepository;
    private final AppUserRepository userRepository;

    public NcMasterService(
            NcMasterRepository ncMasterRepository,
            PcbaNoteRepository pcbaNoteRepository,
            AppUserRepository userRepository
    ) {
        this.ncMasterRepository = ncMasterRepository;
        this.pcbaNoteRepository = pcbaNoteRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NcMasterResponse> findAll() {
        Map<String, String> notes = notesByPart();
        return ncMasterRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(record -> toResponse(record, notes.get(partKey(record.getPcbaPartNumber()))))
                .toList();
    }

    /**
     * Live duplicate check for the NC form, so concurrent editors see a collision
     * before they submit rather than getting a conflict from {@link #create}.
     */
    @Transactional(readOnly = true)
    public NcNumberAvailabilityResponse checkNcNumber(String ncNumber, Long excludeNcId) {
        String normalized = normalizeNumber(ncNumber);
        if (normalized.isEmpty()) {
            return new NcNumberAvailabilityResponse(normalized, false);
        }
        boolean exists = excludeNcId == null
                ? ncMasterRepository.existsByNcNumberIgnoreCase(normalized)
                : ncMasterRepository.existsByNcNumberIgnoreCaseAndNcIdNot(normalized, excludeNcId);
        return new NcNumberAvailabilityResponse(normalized, exists);
    }

    @Transactional(readOnly = true)
    public PcbaNoteLookupResponse lookupPcbaNote(String partNumber) {
        String part = blankToNull(partNumber);
        if (part == null) {
            return new PcbaNoteLookupResponse(false, null, "", null);
        }
        if (!PcbaPartNumbers.isValid(part)) {
            throw new NcMasterValidationException(PcbaPartNumbers.MESSAGE);
        }
        return pcbaNoteRepository.findByPcbaPartNumberIgnoreCase(part)
                .map(note -> new PcbaNoteLookupResponse(true, note.getPcbaNoteId(), note.getPcbaPartNumber(), note.getPcbaNotes()))
                .orElseGet(() -> new PcbaNoteLookupResponse(false, null, part, null));
    }

    public NcMasterResponse create(NcMasterRequest request, Long userId) {
        String ncNumber = normalizeNumber(request.ncNumber());
        if (ncMasterRepository.existsByNcNumberIgnoreCase(ncNumber)) {
            throw new NcNumberExistsException(ncNumber);
        }
        NcMaster record = new NcMaster();
        applyRequest(record, request, ncNumber);
        AppUser user = userRepository.findById(userId).orElse(null);
        record.setCreatedByUserId(user == null ? null : user.getUserId());
        record.setCreatedBy(user == null ? "Unknown" : user.getDisplayName());
        NcMaster saved = ncMasterRepository.save(record);
        String pcbaNotes = syncPcbaNotes(saved.getPcbaPartNumber(), request.pcbaNotes());
        return toResponse(saved, pcbaNotes);
    }

    public NcMasterResponse update(Long id, NcMasterRequest request) {
        NcMaster record = ncMasterRepository.findById(id).orElseThrow(() -> new NcMasterNotFoundException(id));
        String ncNumber = normalizeNumber(request.ncNumber());
        if (ncMasterRepository.existsByNcNumberIgnoreCaseAndNcIdNot(ncNumber, id)) {
            throw new NcNumberExistsException(ncNumber);
        }
        applyRequest(record, request, ncNumber);
        NcMaster saved = ncMasterRepository.save(record);
        String pcbaNotes = syncPcbaNotes(saved.getPcbaPartNumber(), request.pcbaNotes());
        return toResponse(saved, pcbaNotes);
    }

    private void applyRequest(NcMaster record, NcMasterRequest request, String ncNumber) {
        String partNumber = blankToNull(request.pcbaPartNumber());
        if (partNumber != null && !PcbaPartNumbers.isValid(partNumber)) {
            throw new NcMasterValidationException(PcbaPartNumbers.MESSAGE);
        }
        record.setNcNumber(ncNumber);
        record.setPcbPartNumber(blankToNull(request.pcbPartNumber()));
        record.setPcbRev(blankToNull(request.pcbRev()));
        record.setPcbaPartNumber(partNumber);
        record.setPcbaRev(blankToNull(request.pcbaRev()));
        record.setNotes(blankToNull(request.notes()));
        record.setNcAlert(blankToNull(request.ncAlert()));
    }

    /**
     * A null notes argument means the form never opened the notes editor, so the
     * table is left alone. A value is written only because an NC was saved.
     */
    private String syncPcbaNotes(String partNumber, String requestedNotes) {
        if (partNumber == null) {
            return null;
        }
        var existing = pcbaNoteRepository.findByPcbaPartNumberIgnoreCase(partNumber);
        if (requestedNotes == null) {
            return existing.map(PcbaNote::getPcbaNotes).orElse(null);
        }
        String text = blankToNull(requestedNotes);
        if (existing.isEmpty()) {
            if (text == null) {
                return null;
            }
            PcbaNote created = new PcbaNote();
            created.setPcbaPartNumber(partNumber);
            created.setPcbaNotes(text);
            return pcbaNoteRepository.save(created).getPcbaNotes();
        }
        PcbaNote row = existing.get();
        row.setPcbaNotes(text);
        return pcbaNoteRepository.save(row).getPcbaNotes();
    }

    private Map<String, String> notesByPart() {
        Map<String, String> notes = new HashMap<>();
        for (PcbaNote note : pcbaNoteRepository.findAll()) {
            notes.put(partKey(note.getPcbaPartNumber()), note.getPcbaNotes());
        }
        return notes;
    }

    private NcMasterResponse toResponse(NcMaster record, String pcbaNotes) {
        return new NcMasterResponse(
                record.getNcId(),
                record.getNcNumber(),
                record.getPcbPartNumber(),
                record.getPcbRev(),
                record.getPcbaPartNumber(),
                record.getPcbaRev(),
                record.getNotes(),
                record.getNcAlert(),
                pcbaNotes,
                record.getCreatedBy(),
                record.getCreatedByUserId(),
                record.getCreatedAt()
        );
    }

    private static String partKey(String partNumber) {
        return partNumber == null ? "" : partNumber.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNumber(String ncNumber) {
        return ncNumber.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
