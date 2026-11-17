package com.pnc.masters.vendor;

import com.pnc.masters.party.PartyChoice;
import com.pnc.masters.party.PartyList;
import com.pnc.masters.party.PartyLookup;
import com.pnc.masters.party.api.PartyCodeExistsException;
import com.pnc.masters.party.api.PartyNotFoundException;
import com.pnc.masters.security.AppUser;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.vendor.api.VendorRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

    @Mock private VendorRepository vendors;
    @Mock private PartyLookup catalog;
    @Mock private AppUserRepository users;

    private VendorService service;

    @BeforeEach
    void setUp() {
        service = new VendorService(vendors, catalog, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createStoresTheVendor() {
        when(vendors.existsByCodeIgnoreCase("PCB-1")).thenReturn(false);
        when(catalog.resolve(PartyList.VENDOR, 10L, 20L)).thenReturn(new PartyChoice(10L, "PCB", 20L, "PCB Materials"));
        when(vendors.save(any(Vendor.class))).thenAnswer(invocation -> {
            Vendor saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(catalog.names(any())).thenReturn(Map.of(10L, "PCB", 20L, "PCB Materials"));
        when(users.findAllById(any())).thenAnswer(invocation -> user(invocation.getArgument(0)));

        var response = service.create(3L, request("PCB-1", "North Shop", 10L, 20L));

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.code()).isEqualTo("PCB-1");
        assertThat(response.typeName()).isEqualTo("PCB");
        assertThat(response.subtypeName()).isEqualTo("PCB Materials");
        assertThat(response.createdBy()).isEqualTo("Ada");
        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendors).save(captor.capture());
        Vendor saved = captor.getValue();
        assertThat(saved.getContactEmail()).isEqualTo("ann@shop.test");
        assertThat(saved.getCreatedBy()).isEqualTo(3L);
        assertThat(saved.getUpdatedBy()).isEqualTo(3L);
        assertThat(saved.getCreatedDate()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void updateChangesTheVendor() {
        Vendor existing = stored(5L, "PCB-1");
        when(vendors.findById(5L)).thenReturn(Optional.of(existing));
        when(vendors.existsByCodeIgnoreCaseAndVendorIdNot("PCB-2", 5L)).thenReturn(false);
        when(catalog.resolve(PartyList.VENDOR, 10L, 21L)).thenReturn(new PartyChoice(10L, "PCB", 21L, "PCB"));
        when(vendors.save(any(Vendor.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(catalog.names(any())).thenReturn(Map.of(10L, "PCB", 21L, "PCB"));
        when(users.findAllById(any())).thenAnswer(invocation -> user(invocation.getArgument(0)));

        var response = service.update(5L, 8L, request("PCB-2", "South Shop", 10L, 21L));

        assertThat(response.code()).isEqualTo("PCB-2");
        assertThat(response.name()).isEqualTo("South Shop");
        assertThat(existing.getUpdatedBy()).isEqualTo(8L);
        assertThat(existing.getSubtypeEntryId()).isEqualTo(21L);
        assertThat(existing.getCreatedBy()).isEqualTo(3L);
    }

    @Test
    void deleteSoftDeletesTheVendor() {
        Vendor existing = stored(5L, "PCB-1");
        when(vendors.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        assertThat(existing.isDeleted()).isTrue();
        verify(vendors).save(existing);
    }

    @Test
    void createRejectsADuplicateCode() {
        when(vendors.existsByCodeIgnoreCase("PCB-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(3L, request("PCB-1", "North Shop", 10L, 20L)))
                .isInstanceOf(PartyCodeExistsException.class)
                .hasMessage("That code is already in use.");
        verify(vendors, never()).save(any());
    }

    @Test
    void findByIdSkipsADeletedVendor() {
        Vendor existing = stored(5L, "PCB-1");
        existing.setDeleted(true);
        when(vendors.findById(5L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.findById(5L))
                .isInstanceOf(PartyNotFoundException.class)
                .hasMessage("Vendor was not found.");
    }

    private static VendorRequest request(String code, String name, Long typeId, Long subtypeId) {
        return new VendorRequest(code, name, typeId, subtypeId, "Ann", "1 Main", "ann@shop.test", "555");
    }

    private static Vendor stored(Long id, String code) {
        Vendor vendor = new Vendor();
        vendor.setId(id);
        vendor.setCode(code);
        vendor.setName("North Shop");
        vendor.setTypeEntryId(10L);
        vendor.setSubtypeEntryId(20L);
        vendor.setCreatedBy(3L);
        vendor.setUpdatedBy(3L);
        vendor.setCreatedDate(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        vendor.setUpdatedDate(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        return vendor;
    }

    private static List<AppUser> user(Collection<Long> ids) {
        return ids.stream().map(id -> {
            AppUser user = new AppUser();
            user.setUserId(id);
            user.setDisplayName(id == 8L ? "Sam" : "Ada");
            return user;
        }).toList();
    }
}
