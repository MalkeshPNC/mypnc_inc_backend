package com.pnc.masters.ncmaster;

import com.pnc.masters.ncmaster.api.NcMasterNotFoundException;
import com.pnc.masters.ncmaster.api.NcMasterRequest;
import com.pnc.masters.ncmaster.api.NcMasterResponse;
import com.pnc.masters.ncmaster.api.NcNumberAvailabilityResponse;
import com.pnc.masters.ncmaster.api.NcNumberExistsException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class NcMasterService {

    private final NcMasterRepository ncMasterRepository;
    private final AppUserRepository userRepository;

    public NcMasterService(NcMasterRepository ncMasterRepository, AppUserRepository userRepository) {
        this.ncMasterRepository = ncMasterRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<NcMasterResponse> findAll() {
        return ncMasterRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
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
        return toResponse(ncMasterRepository.save(record));
    }

    public NcMasterResponse update(Long id, NcMasterRequest request) {
        NcMaster record = ncMasterRepository.findById(id).orElseThrow(() -> new NcMasterNotFoundException(id));
        String ncNumber = normalizeNumber(request.ncNumber());
        if (ncMasterRepository.existsByNcNumberIgnoreCaseAndNcIdNot(ncNumber, id)) {
            throw new NcNumberExistsException(ncNumber);
        }
        applyRequest(record, request, ncNumber);
        return toResponse(ncMasterRepository.save(record));
    }

    private void applyRequest(NcMaster record, NcMasterRequest request, String ncNumber) {
        record.setNcNumber(ncNumber);
        record.setPcbPartNumber(blankToNull(request.pcbPartNumber()));
        record.setPcbRev(blankToNull(request.pcbRev()));
        record.setPcbaPartNumber(blankToNull(request.pcbaPartNumber()));
        record.setPcbaRev(blankToNull(request.pcbaRev()));
        record.setPcbaAlert(blankToNull(request.pcbaAlert()));
        record.setNotes(blankToNull(request.notes()));
        record.setNcAlert(blankToNull(request.ncAlert()));
    }

    private NcMasterResponse toResponse(NcMaster record) {
        return new NcMasterResponse(
                record.getNcId(),
                record.getNcNumber(),
                record.getPcbPartNumber(),
                record.getPcbRev(),
                record.getPcbaPartNumber(),
                record.getPcbaRev(),
                record.getPcbaAlert(),
                record.getNotes(),
                record.getNcAlert(),
                record.getCreatedBy(),
                record.getCreatedByUserId(),
                record.getCreatedAt()
        );
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
