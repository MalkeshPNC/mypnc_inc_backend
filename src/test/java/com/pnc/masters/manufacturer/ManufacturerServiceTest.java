package com.pnc.masters.manufacturer;

import com.pnc.masters.manufacturer.api.ManufacturerRequest;
import com.pnc.masters.party.PartyChoice;
import com.pnc.masters.party.PartyList;
import com.pnc.masters.party.PartyLookup;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManufacturerServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    @Mock private ManufacturerRepository manufacturers;
    @Mock private PartyLookup catalog;
    @Mock private AppUserRepository users;

    private ManufacturerService service;

    @BeforeEach
    void setUp() {
        service = new ManufacturerService(manufacturers, catalog, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createStoresAManufacturerWithTypeOnly() {
        when(manufacturers.existsByCodeIgnoreCase("ASM-1")).thenReturn(false);
        when(catalog.resolve(PartyList.MANUFACTURER, 10L, null)).thenReturn(new PartyChoice(10L, "Assembly", null, null));
        when(manufacturers.save(any(Manufacturer.class))).thenAnswer(invocation -> {
            Manufacturer saved = invocation.getArgument(0);
            saved.setId(6L);
            return saved;
        });
        when(catalog.names(any())).thenReturn(Map.of(10L, "Assembly"));
        when(users.findAllById(any())).thenAnswer(invocation -> users(invocation.getArgument(0)));

        var response = service.create(3L, new ManufacturerRequest(
                "ASM-1", "Line House", 10L, null, null, null, null, null));

        assertThat(response.id()).isEqualTo(6L);
        assertThat(response.typeName()).isEqualTo("Assembly");
        assertThat(response.subtypeEntryId()).isNull();
        assertThat(response.subtypeName()).isNull();
        ArgumentCaptor<Manufacturer> captor = ArgumentCaptor.forClass(Manufacturer.class);
        verify(manufacturers).save(captor.capture());
        assertThat(captor.getValue().getSubtypeEntryId()).isNull();
        assertThat(captor.getValue().getTypeEntryId()).isEqualTo(10L);
    }

    private static List<AppUser> users(Collection<Long> ids) {
        return ids.stream().map(id -> {
            AppUser user = new AppUser();
            user.setUserId(id);
            user.setDisplayName("Ada");
            return user;
        }).toList();
    }
}
