package com.pnc.masters.pcbinventory;

import com.pnc.masters.manufacturer.Manufacturer;
import com.pnc.masters.manufacturer.ManufacturerRepository;
import com.pnc.masters.pcbinventory.api.PcbInventoryDetailResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryItemResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryLookupOption;
import com.pnc.masters.pcbinventory.api.PcbInventoryLookupsResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryNotFoundException;
import com.pnc.masters.pcbinventory.api.PcbInventoryReceiptRequest;
import com.pnc.masters.pcbinventory.api.PcbInventoryRequest;
import com.pnc.masters.pcbinventory.api.PcbInventoryTransactionRequest;
import com.pnc.masters.pcbinventory.api.PcbInventoryTxnResponse;
import com.pnc.masters.pcbinventory.api.PcbInventoryValidationException;
import com.pnc.masters.security.AppUserRepository;
import com.pnc.masters.vendor.Vendor;
import com.pnc.masters.vendor.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class PcbInventoryService {

    public static final Set<String> ITEM_TYPES = Set.of("Aluminum", "Core", "Laminate", "Unclad");
    public static final Set<String> INITIAL_NAMES = Set.of(
            "Lalo Patel", "Lisa Martin", "Ravin Sangani", "Rebecca Stango");

    private static final String ALLOCATE = "ALLOCATE_JOB";
    private static final String ON_ORDER = "ON_ORDER";
    private static final String ADJUSTMENT = "ADJUSTMENT";
    private static final String RECEIVE = "RECEIVE";

    private final PcbInventoryItemRepository items;
    private final PcbInventoryTxnRepository transactions;
    private final ManufacturerRepository manufacturers;
    private final VendorRepository vendors;
    private final AppUserRepository users;

    public PcbInventoryService(PcbInventoryItemRepository items,
                               PcbInventoryTxnRepository transactions,
                               ManufacturerRepository manufacturers,
                               VendorRepository vendors,
                               AppUserRepository users) {
        this.items = items;
        this.transactions = transactions;
        this.manufacturers = manufacturers;
        this.vendors = vendors;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<PcbInventoryItemResponse> list(String view) {
        String normalized = view == null ? "all" : view.trim().toLowerCase(Locale.ROOT);
        List<PcbInventoryItem> rows = switch (normalized) {
            case "all", "" -> items.findAllByOrderByItemIdDesc();
            case "reorder" -> items.findReorder();
            case "receive" -> items.findReceiving();
            default -> throw new PcbInventoryValidationException("View must be all, reorder, or receive.");
        };
        return toItems(rows);
    }

    @Transactional(readOnly = true)
    public PcbInventoryDetailResponse findById(Long id) {
        PcbInventoryItem item = getItem(id);
        List<PcbInventoryItem> rows = List.of(item);
        return new PcbInventoryDetailResponse(toItem(item, manufacturerNames(rows), namesByKey(rows)), transactionsFor(item));
    }

    @Transactional(readOnly = true)
    public PcbInventoryLookupsResponse lookups() {
        List<PcbInventoryLookupOption> manufacturerOptions = manufacturers
                .findByDeletedFalseOrderByCreatedDateDescManufacturerIdDesc().stream()
                .sorted(Comparator.comparing(Manufacturer::getName, String.CASE_INSENSITIVE_ORDER))
                .map(row -> new PcbInventoryLookupOption(row.getId(), row.getName()))
                .toList();
        List<PcbInventoryLookupOption> vendorOptions = vendors
                .findByDeletedFalseOrderByCreatedDateDescVendorIdDesc().stream()
                .sorted(Comparator.comparing(Vendor::getName, String.CASE_INSENSITIVE_ORDER))
                .map(row -> new PcbInventoryLookupOption(row.getId(), row.getName()))
                .toList();
        return new PcbInventoryLookupsResponse(manufacturerOptions, vendorOptions);
    }

    public PcbInventoryItemResponse create(PcbInventoryRequest request) {
        if (request.manufacturerId() == null) {
            throw new PcbInventoryValidationException("Choose a manufacturer.");
        }
        PcbInventoryItem item = new PcbInventoryItem();
        applyMaster(item, request);
        applyManufacturer(item, request.manufacturerId());
        item.setAvailable(0);
        item.setAllocated(0);
        item.setOnOrder(0);
        return toItem(items.save(item), manufacturerNames(List.of(item)), Map.of());
    }

    public PcbInventoryItemResponse update(Long id, PcbInventoryRequest request) {
        PcbInventoryItem item = getItem(id);
        applyMaster(item, request);
        if (request.manufacturerId() != null) {
            applyManufacturer(item, request.manufacturerId());
        } else if (item.getManufacturerId() != null || blankToNull(item.getManufacturerName()) == null) {
            throw new PcbInventoryValidationException("Choose a manufacturer.");
        }
        item.setAvailable(requiredQty(request.available(), "Available"));
        item.setAllocated(requiredQty(request.allocated(), "Allocated"));
        item.setOnOrder(requiredQty(request.onOrder(), "On order"));
        PcbInventoryItem saved = items.save(item);
        List<PcbInventoryItem> rows = List.of(saved);
        return toItem(saved, manufacturerNames(rows), namesByKey(rows));
    }

    public PcbInventoryItemResponse transact(Long id, PcbInventoryTransactionRequest request) {
        PcbInventoryItem item = getItem(id);
        String type = normalizeType(request.transType());
        int quantity = request.quantity();
        String initialName = requireInitial(request.initialName());
        String reference = blankToNull(request.reference());
        String details = blankToNull(request.details());

        PcbInventoryTxn txn = new PcbInventoryTxn();
        txn.setItemId(item.getItemId());
        txn.setTransType(type);
        txn.setSourceName(details);
        txn.setSourceNumber(reference);
        txn.setInitialName(initialName);
        txn.setActionBy(initialName);
        txn.setActionDate(LocalDateTime.now());
        txn.setInQty(0);
        txn.setOutQty(0);

        switch (type) {
            case ALLOCATE -> allocate(item, txn, quantity);
            case ON_ORDER -> placeOnOrder(item, txn, request, quantity);
            case ADJUSTMENT -> adjust(item, txn, request.direction(), quantity);
            default -> throw new PcbInventoryValidationException("Choose Allocate to Job, On Order, or Adjustment.");
        }

        items.save(item);
        transactions.save(txn);
        return toItem(item, manufacturerNames(List.of(item)), Map.of());
    }

    public PcbInventoryItemResponse receive(Long id, PcbInventoryReceiptRequest request, Long userId) {
        PcbInventoryItem item = getItem(id);
        int quantity = request.quantity();
        if (item.getOnOrder() <= 0 || quantity > item.getOnOrder()) {
            throw new PcbInventoryValidationException("Quantity is greater than the quantity on order.");
        }
        Vendor vendor = requireVendor(request.vendorId());
        item.setOnOrder(item.getOnOrder() - quantity);
        item.setAvailable(item.getAvailable() + quantity);

        PcbInventoryTxn txn = new PcbInventoryTxn();
        txn.setItemId(item.getItemId());
        txn.setTransType(RECEIVE);
        txn.setVendorId(vendor.getId());
        txn.setVendorName(null);
        txn.setSourceName(blankToNull(request.details()));
        txn.setSourceNumber(request.receiptNo().trim());
        txn.setInQty(quantity);
        txn.setOutQty(0);
        txn.setActionBy(actorName(userId));
        txn.setActionDate(LocalDateTime.now());

        items.save(item);
        transactions.save(txn);
        return toItem(item, manufacturerNames(List.of(item)), Map.of());
    }

    private void allocate(PcbInventoryItem item, PcbInventoryTxn txn, int quantity) {
        if (item.getAvailable() <= 0 || quantity > item.getAvailable()) {
            throw new PcbInventoryValidationException("Not enough available quantity to allocate.");
        }
        item.setAvailable(item.getAvailable() - quantity);
        item.setAllocated(item.getAllocated() + quantity);
        txn.setOutQty(quantity);
    }

    private void placeOnOrder(PcbInventoryItem item, PcbInventoryTxn txn, PcbInventoryTransactionRequest request, int quantity) {
        Vendor vendor = requireVendor(request.vendorId());
        if (request.price() == null) {
            throw new PcbInventoryValidationException("Price is required for On Order.");
        }
        item.setOnOrder(item.getOnOrder() + quantity);
        txn.setVendorId(vendor.getId());
        txn.setVendorName(null);
        txn.setPrice(money(request.price()));
        txn.setInQty(quantity);
    }

    private void adjust(PcbInventoryItem item, PcbInventoryTxn txn, String direction, int quantity) {
        String normalized = direction == null ? "" : direction.trim().toUpperCase(Locale.ROOT);
        if ("IN".equals(normalized)) {
            item.setAvailable(item.getAvailable() + quantity);
            txn.setInQty(quantity);
            return;
        }
        if ("OUT".equals(normalized)) {
            if (quantity > item.getAvailable()) {
                throw new PcbInventoryValidationException("Not enough available quantity for this adjustment.");
            }
            item.setAvailable(item.getAvailable() - quantity);
            txn.setOutQty(quantity);
            return;
        }
        throw new PcbInventoryValidationException("Adjustment must be IN or OUT.");
    }

    private String displayManufacturer(PcbInventoryItem item,
                                       Map<Long, String> manufacturerNames,
                                       Map<String, List<Manufacturer>> byName) {
        if (item.getManufacturerId() != null) {
            String live = manufacturerNames.get(item.getManufacturerId());
            if (live != null && !live.isBlank()) {
                return live;
            }
        }
        String stored = blankToNull(item.getManufacturerName());
        if (stored == null) {
            return null;
        }
        List<Manufacturer> matches = byName.getOrDefault(PcbInventoryNames.key(stored), List.of());
        if (matches.size() == 1) {
            return matches.get(0).getName();
        }
        return stored;
    }

    private void applyMaster(PcbInventoryItem item, PcbInventoryRequest request) {
        String itemType = request.itemType().trim();
        if (!ITEM_TYPES.contains(itemType)) {
            throw new PcbInventoryValidationException("Item type must be Aluminum, Core, Laminate, or Unclad.");
        }
        item.setDescription(request.description().trim());
        item.setItemType(itemType);
        item.setSheetSize(request.sheetSize().trim());
        item.setLocation(request.location().trim());
        item.setLeadTime(blankToNull(request.leadTime()));
        item.setStdPrice(request.stdPrice() == null ? null : money(request.stdPrice()));
        item.setReorderLevel(request.reorderLevel());
        item.setReorderQty(request.reorderQty());
        item.setAlertOnItem(blankToNull(request.alertOnItem()));
        item.setComments(blankToNull(request.comments()));
    }

    private void applyManufacturer(PcbInventoryItem item, Long manufacturerId) {
        requireManufacturer(manufacturerId);
        item.setManufacturerId(manufacturerId);
        item.setManufacturerName(null);
    }

    private Manufacturer requireManufacturer(Long id) {
        Manufacturer manufacturer = manufacturers.findById(id)
                .orElseThrow(() -> new PcbInventoryValidationException("Choose a manufacturer."));
        if (manufacturer.isDeleted()) {
            throw new PcbInventoryValidationException("Choose a manufacturer.");
        }
        return manufacturer;
    }

    private Vendor requireVendor(Long id) {
        if (id == null) {
            throw new PcbInventoryValidationException("Vendor is required.");
        }
        Vendor vendor = vendors.findById(id)
                .orElseThrow(() -> new PcbInventoryValidationException("Choose a vendor."));
        if (vendor.isDeleted()) {
            throw new PcbInventoryValidationException("Choose a vendor.");
        }
        return vendor;
    }

    private String requireInitial(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (!INITIAL_NAMES.contains(trimmed)) {
            throw new PcbInventoryValidationException("Choose an initial name.");
        }
        return trimmed;
    }

    private String normalizeType(String transType) {
        String normalized = transType == null ? "" : transType.trim().toUpperCase(Locale.ROOT);
        if (ALLOCATE.equals(normalized) || ON_ORDER.equals(normalized) || ADJUSTMENT.equals(normalized)) {
            return normalized;
        }
        throw new PcbInventoryValidationException("Choose Allocate to Job, On Order, or Adjustment.");
    }

    private int requiredQty(Integer value, String label) {
        if (value == null) {
            throw new PcbInventoryValidationException(label + " is required.");
        }
        return value;
    }

    private String actorName(Long userId) {
        if (userId == null) {
            return "Unknown";
        }
        return users.findById(userId)
                .map(user -> blankToNull(user.getDisplayName()) == null ? user.getEmail() : user.getDisplayName().trim())
                .orElse("User " + userId);
    }

    private PcbInventoryItem getItem(Long id) {
        return items.findById(id).orElseThrow(() -> new PcbInventoryNotFoundException(id));
    }

    private List<PcbInventoryItemResponse> toItems(List<PcbInventoryItem> rows) {
        Map<Long, String> names = manufacturerNames(rows);
        Map<String, List<Manufacturer>> byName = namesByKey(rows);
        return rows.stream().map(row -> toItem(row, names, byName)).toList();
    }

    private PcbInventoryItemResponse toItem(PcbInventoryItem item,
                                            Map<Long, String> manufacturerNames,
                                            Map<String, List<Manufacturer>> byName) {
        return new PcbInventoryItemResponse(
                item.getItemId(),
                item.getDescription(),
                item.getItemType(),
                item.getLocation(),
                item.getManufacturerId(),
                displayManufacturer(item, manufacturerNames, byName),
                item.getSheetSize(),
                item.getAvailable(),
                item.getAllocated(),
                item.getOnOrder(),
                item.getLeadTime(),
                item.getReorderLevel(),
                item.getReorderQty(),
                item.getAlertOnItem(),
                item.getStdPrice(),
                item.getComments()
        );
    }

    private List<PcbInventoryTxnResponse> transactionsFor(PcbInventoryItem item) {
        List<PcbInventoryTxn> rows = transactions.findByItemIdOrderByActionDateDescTxnIdDesc(item.getItemId());
        Map<Long, String> byId = vendorNames(rows);
        Map<String, List<Vendor>> byName = vendorNamesByKey(rows);
        return rows.stream().map(row -> new PcbInventoryTxnResponse(
                row.getTxnId(),
                displayType(row.getTransType()),
                displayVendor(row, byId, byName),
                row.getSourceName(),
                row.getSourceNumber(),
                row.getInQty(),
                row.getOutQty(),
                row.getPrice(),
                row.getActionBy(),
                row.getActionDate()
        )).toList();
    }

    private String displayVendor(PcbInventoryTxn row, Map<Long, String> byId, Map<String, List<Vendor>> byName) {
        if (row.getVendorId() != null) {
            String live = byId.get(row.getVendorId());
            if (live != null && !live.isBlank()) {
                return live;
            }
        }
        String stored = blankToNull(row.getVendorName());
        if (stored == null) {
            return null;
        }
        List<Vendor> matches = byName.getOrDefault(PcbInventoryNames.key(stored), List.of());
        if (matches.size() == 1) {
            return matches.get(0).getName();
        }
        return stored;
    }

    private Map<String, List<Manufacturer>> namesByKey(List<PcbInventoryItem> rows) {
        boolean needsMatch = rows.stream().anyMatch(row ->
                row.getManufacturerId() == null && blankToNull(row.getManufacturerName()) != null);
        if (!needsMatch) {
            return Map.of();
        }
        Map<String, List<Manufacturer>> byName = new HashMap<>();
        for (Manufacturer manufacturer : manufacturers.findByDeletedFalseOrderByCreatedDateDescManufacturerIdDesc()) {
            String key = PcbInventoryNames.key(manufacturer.getName());
            if (!key.isEmpty()) {
                byName.computeIfAbsent(key, ignored -> new ArrayList<>()).add(manufacturer);
            }
        }
        return byName;
    }

    private Map<String, List<Vendor>> vendorNamesByKey(List<PcbInventoryTxn> rows) {
        boolean needsMatch = rows.stream().anyMatch(row ->
                row.getVendorId() == null && blankToNull(row.getVendorName()) != null);
        if (!needsMatch) {
            return Map.of();
        }
        Map<String, List<Vendor>> byName = new HashMap<>();
        for (Vendor vendor : vendors.findByDeletedFalseOrderByCreatedDateDescVendorIdDesc()) {
            String key = PcbInventoryNames.key(vendor.getName());
            if (!key.isEmpty()) {
                byName.computeIfAbsent(key, ignored -> new ArrayList<>()).add(vendor);
            }
        }
        return byName;
    }

    private Map<Long, String> manufacturerNames(List<PcbInventoryItem> rows) {
        Set<Long> ids = rows.stream()
                .map(PcbInventoryItem::getManufacturerId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return manufacturers.findAllById(ids).stream()
                .collect(Collectors.toMap(Manufacturer::getId, Manufacturer::getName));
    }

    private Map<Long, String> vendorNames(List<PcbInventoryTxn> rows) {
        Set<Long> ids = rows.stream()
                .map(PcbInventoryTxn::getVendorId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return vendors.findAllById(ids).stream()
                .collect(Collectors.toMap(Vendor::getId, Vendor::getName));
    }

    private static String displayType(String transType) {
        return switch (transType) {
            case ALLOCATE -> "Allocate to Job";
            case ON_ORDER -> "On Order";
            case ADJUSTMENT -> "Adjustment";
            case RECEIVE -> "Receive";
            default -> transType;
        };
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
