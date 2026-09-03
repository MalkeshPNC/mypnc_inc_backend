package com.pnc.masters.ncmaster;

import com.pnc.masters.ncmaster.api.NcMasterRequest;
import com.pnc.masters.ncmaster.api.NcNumberExistsException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NcMasterServiceTest {

    @Mock private NcMasterRepository ncMasterRepository;
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

        var response = service.create(new NcMasterRequest(
                " nc-100 ",
                "PCB-1",
                "A",
                "PCBA-1",
                "B",
                "pcb alert",
                "notes",
                "nc alert"
        ), 7L);

        assertThat(response.ncNumber()).isEqualTo("NC-100");
        assertThat(response.createdBy()).isEqualTo("Ada Lovelace");
        assertThat(response.createdByUserId()).isEqualTo(7L);
        assertThat(response.pcbPartNumber()).isEqualTo("PCB-1");
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
