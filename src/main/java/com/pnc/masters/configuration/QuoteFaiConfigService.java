package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.FaiConfigValidationException;
import com.pnc.masters.configuration.api.QuoteFaiConfigRequest;
import com.pnc.masters.configuration.api.QuoteFaiConfigResponse;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@Transactional
public class QuoteFaiConfigService {

    public static final String DEFAULT_HEADING =
            "Optional Reports/Paperwork: Add pricing(if needed per Drawing/Quality Clauses). Unpriced PO will be considered as waived.";

    public static final String DEFAULT_IMPORTANT_NOTES =
            "Important Notes : Quoated with ENIG surface finish.";

    private static final BigDecimal DEFAULT_PNC_PCB = new BigDecimal("300.00");
    private static final BigDecimal DEFAULT_PNC_PCBA = new BigDecimal("210.00");
    private static final BigDecimal DEFAULT_AS9102_PCB = new BigDecimal("450.00");
    private static final BigDecimal DEFAULT_AS9102_PCBA = new BigDecimal("315.00");

    private final QuoteFaiConfigRepository repository;
    private final AppUserRepository userRepository;

    public QuoteFaiConfigService(QuoteFaiConfigRepository repository, AppUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = false)
    public QuoteFaiConfigResponse find() {
        return toResponse(getOrSeed());
    }

    public QuoteFaiConfigResponse update(QuoteFaiConfigRequest request, Long userId) {
        QuoteFaiConfig config = repository.findById(QuoteFaiConfig.SINGLETON_ID).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        String actor = displayName(userId);
        if (config == null) {
            config = new QuoteFaiConfig();
            config.setFaiId(QuoteFaiConfig.SINGLETON_ID);
            config.setCreatedAt(now);
            config.setCreatedBy(actor);
        } else {
            config.setUpdatedAt(now);
            config.setUpdatedBy(actor);
        }
        config.setHeadingNotes(blankToNull(request.headingNotes()));
        config.setImportantNotes(blankToNull(request.importantNotes()));
        config.setPncFaiPcb(requirePrice(request.pncFaiPcb(), "PNC FAI PCB"));
        config.setPncFaiPcba(requirePrice(request.pncFaiPcba(), "PNC FAI PCBA"));
        config.setAs9102FaiPcb(requirePrice(request.as9102FaiPcb(), "AS9102 FAI PCB"));
        config.setAs9102FaiPcba(requirePrice(request.as9102FaiPcba(), "AS9102 FAI PCBA"));
        return toResponse(repository.save(config));
    }

    private QuoteFaiConfig getOrSeed() {
        return repository.findById(QuoteFaiConfig.SINGLETON_ID).orElseGet(() -> {
            QuoteFaiConfig config = new QuoteFaiConfig();
            config.setFaiId(QuoteFaiConfig.SINGLETON_ID);
            config.setHeadingNotes(DEFAULT_HEADING);
            config.setImportantNotes(DEFAULT_IMPORTANT_NOTES);
            config.setPncFaiPcb(DEFAULT_PNC_PCB);
            config.setPncFaiPcba(DEFAULT_PNC_PCBA);
            config.setAs9102FaiPcb(DEFAULT_AS9102_PCB);
            config.setAs9102FaiPcba(DEFAULT_AS9102_PCBA);
            config.setCreatedAt(LocalDateTime.now());
            config.setCreatedBy("system");
            return repository.save(config);
        });
    }

    private String displayName(Long userId) {
        AppUser user = userId == null ? null : userRepository.findById(userId).orElse(null);
        return user == null ? "Unknown" : user.getDisplayName();
    }

    private static BigDecimal requirePrice(BigDecimal value, String field) {
        if (value == null) {
            throw new FaiConfigValidationException(field + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new FaiConfigValidationException(field + " cannot be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static QuoteFaiConfigResponse toResponse(QuoteFaiConfig config) {
        return new QuoteFaiConfigResponse(
                config.getHeadingNotes(),
                config.getImportantNotes(),
                config.getPncFaiPcb(),
                config.getPncFaiPcba(),
                config.getAs9102FaiPcb(),
                config.getAs9102FaiPcba(),
                config.getCreatedAt(),
                config.getCreatedBy(),
                config.getUpdatedAt(),
                config.getUpdatedBy()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
