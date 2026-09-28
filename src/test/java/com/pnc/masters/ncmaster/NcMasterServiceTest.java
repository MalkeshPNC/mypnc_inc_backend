package com.pnc.masters.ncmaster;

import com.pnc.masters.ncmaster.api.NcMasterRequest;
import com.pnc.masters.ncmaster.api.NcMasterValidationException;
import com.pnc.masters.ncmaster.api.NcNumberExistsException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NcMasterServiceTest {

    @Mock private NcMasterRepository ncMasterRepository;
    @Mock private PcbaNoteRepository pcbaNoteRepository;
    @Mock private AppUserRepository userRepository;

    @InjectMocks
    private NcMasterService service;

    @Test
    void createStoresUppercaseNumberAndCreator() {
        AppUser user = new AppUser();
        user.setUserId(7L);
        user.setDisplayName("Ada Lovelace");
        when(ncMasterRepository.existsByNcNumberIgnoreCase("NC-100")).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(ncMasterRepository.save(any(NcMaster.class))).thenAnswer(invocation -> {
            NcMaster saved = invocation.getArgument(0);
            saved.setNcId(1L);
            return saved;
        });
        when(pcbaNoteRepository.findByPcbaPartNumberIgnoreCase("PCBA-1")).thenReturn(Optional.empty());

        var response = service.create(new NcMasterRequest(
                " nc-100 ",
                "PCB-1",
                "A",
                "PCBA-1",
                "B",
                "notes",
                "nc alert",
                null
        ), 7L);

        assertThat(response.ncNumber()).isEqualTo("NC-100");
        assertThat(response.createdBy()).isEqualTo("Ada Lovelace");
        assertThat(response.createdByUserId()).isEqualTo(7L);
        assertThat(response.pcbPartNumber()).isEqualTo("PCB-1");
        verify(pcbaNoteRepository, never()).save(any());
    }

    @Test
    void createWritesPcbaNotesOnlyWhenTheFormSendsThem() {
        when(ncMasterRepository.existsByNcNumberIgnoreCase("NC-100")).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());
        when(ncMasterRepository.save(any(NcMaster.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pcbaNoteRepository.findByPcbaPartNumberIgnoreCase("PCBA-9")).thenReturn(Optional.empty());
        when(pcbaNoteRepository.save(any(PcbaNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new NcMasterRequest(
                "NC-100", null, null, "PCBA-9", null, null, null, "  watch the polarity  "
        ), 7L);

        ArgumentCaptor<PcbaNote> saved = ArgumentCaptor.forClass(PcbaNote.class);
        verify(pcbaNoteRepository).save(saved.capture());
        assertThat(saved.getValue().getPcbaPartNumber()).isEqualTo("PCBA-9");
        assertThat(saved.getValue().getPcbaNotes()).isEqualTo("watch the polarity");
        assertThat(response.pcbaNotes()).isEqualTo("watch the polarity");
    }

    @Test
    void createRejectsAPcbaPartNumberWithUnsupportedCharacters() {
        assertThatThrownBy(() -> service.create(
                new NcMasterRequest("NC-100", null, null, "PCBA 1", null, null, null, "note"),
                1L
        )).isInstanceOf(NcMasterValidationException.class);
        verify(ncMasterRepository, never()).save(any());
        verify(pcbaNoteRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNumber() {
        when(ncMasterRepository.existsByNcNumberIgnoreCase("NC-100")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new NcMasterRequest("NC-100", null, null, null, null, null, null, null),
                1L
        )).isInstanceOf(NcNumberExistsException.class);
    }

    @Test
    void checkNcNumberNormalizesBeforeLookup() {
        when(ncMasterRepository.existsByNcNumberIgnoreCase("NC-100")).thenReturn(true);

        var response = service.checkNcNumber(" nc-100 ", null);

        assertThat(response.ncNumber()).isEqualTo("NC-100");
        assertThat(response.exists()).isTrue();
    }

    @Test
    void checkNcNumberIgnoresTheRecordBeingEdited() {
        when(ncMasterRepository.existsByNcNumberIgnoreCaseAndNcIdNot("NC-200", 2L)).thenReturn(false);

        var response = service.checkNcNumber("NC-200", 2L);

        assertThat(response.exists()).isFalse();
    }

    @Test
    void checkNcNumberTreatsBlankAsAvailable() {
        var response = service.checkNcNumber("   ", null);

        assertThat(response.exists()).isFalse();
    }

    @Test
    void updateRejectsDuplicateNumber() {
        NcMaster existing = new NcMaster();
        existing.setNcId(2L);
        existing.setNcNumber("NC-200");
        when(ncMasterRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(ncMasterRepository.existsByNcNumberIgnoreCaseAndNcIdNot("NC-100", 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                2L,
                new NcMasterRequest("nc-100", null, null, null, null, null, null, null)
        )).isInstanceOf(NcNumberExistsException.class);
    }
}
