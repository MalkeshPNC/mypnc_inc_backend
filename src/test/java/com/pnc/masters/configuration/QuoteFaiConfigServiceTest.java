package com.pnc.masters.configuration;

import com.pnc.masters.configuration.api.FaiConfigValidationException;
import com.pnc.masters.configuration.api.QuoteFaiConfigRequest;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteFaiConfigServiceTest {

    @Mock
    private QuoteFaiConfigRepository repository;

    @Mock
    private AppUserRepository userRepository;

    @InjectMocks
    private QuoteFaiConfigService service;

    @Test
    void findReturnsSeededSingleton() {
        QuoteFaiConfig existing = seededConfig();
        when(repository.findById(QuoteFaiConfig.SINGLETON_ID)).thenReturn(Optional.of(existing));

        var response = service.find();

        assertThat(response.headingNotes()).isEqualTo(QuoteFaiConfigService.DEFAULT_HEADING);
        assertThat(response.importantNotes()).isEqualTo(QuoteFaiConfigService.DEFAULT_IMPORTANT_NOTES);
        assertThat(response.pncFaiPcb()).isEqualByComparingTo("300.00");
        assertThat(response.pncFaiPcba()).isEqualByComparingTo("210.00");
        assertThat(response.as9102FaiPcb()).isEqualByComparingTo("450.00");
        assertThat(response.as9102FaiPcba()).isEqualByComparingTo("315.00");
        assertThat(response.createdBy()).isEqualTo("system");
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesPricesAndStampsUpdatedBy() {
        QuoteFaiConfig existing = seededConfig();
        AppUser user = new AppUser();
        user.setUserId(9L);
        user.setDisplayName("Ada Lovelace");
        when(repository.findById(QuoteFaiConfig.SINGLETON_ID)).thenReturn(Optional.of(existing));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(repository.save(any(QuoteFaiConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(new QuoteFaiConfigRequest(
                "  Updated heading  ",
                "  Red notes  ",
                new BigDecimal("400"),
                new BigDecimal("250.5"),
                new BigDecimal("500"),
                new BigDecimal("350")
        ), 9L);

        assertThat(response.headingNotes()).isEqualTo("Updated heading");
        assertThat(response.importantNotes()).isEqualTo("Red notes");
        assertThat(response.pncFaiPcb()).isEqualByComparingTo("400.00");
        assertThat(response.pncFaiPcba()).isEqualByComparingTo("250.50");
        assertThat(response.as9102FaiPcb()).isEqualByComparingTo("500.00");
        assertThat(response.as9102FaiPcba()).isEqualByComparingTo("350.00");
        assertThat(response.createdBy()).isEqualTo("system");
        assertThat(response.updatedBy()).isEqualTo("Ada Lovelace");
        assertThat(response.updatedAt()).isNotNull();
        ArgumentCaptor<QuoteFaiConfig> captor = ArgumentCaptor.forClass(QuoteFaiConfig.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo("Ada Lovelace");
    }

    @Test
    void updateRejectsMissingPrices() {
        QuoteFaiConfig existing = seededConfig();
        when(repository.findById(QuoteFaiConfig.SINGLETON_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(new QuoteFaiConfigRequest(
                QuoteFaiConfigService.DEFAULT_HEADING,
                QuoteFaiConfigService.DEFAULT_IMPORTANT_NOTES,
                null,
                new BigDecimal("210.00"),
                new BigDecimal("450.00"),
                new BigDecimal("315.00")
        ), 9L)).isInstanceOf(FaiConfigValidationException.class)
                .hasMessageContaining("PNC FAI PCB");
        verify(repository, never()).save(any());
    }

    private static QuoteFaiConfig seededConfig() {
        QuoteFaiConfig config = new QuoteFaiConfig();
        config.setFaiId(QuoteFaiConfig.SINGLETON_ID);
        config.setHeadingNotes(QuoteFaiConfigService.DEFAULT_HEADING);
        config.setImportantNotes(QuoteFaiConfigService.DEFAULT_IMPORTANT_NOTES);
        config.setPncFaiPcb(new BigDecimal("300.00"));
        config.setPncFaiPcba(new BigDecimal("210.00"));
        config.setAs9102FaiPcb(new BigDecimal("450.00"));
        config.setAs9102FaiPcba(new BigDecimal("315.00"));
        config.setCreatedAt(LocalDateTime.of(2026, 9, 18, 12, 0));
        config.setCreatedBy("system");
        return config;
    }
}
