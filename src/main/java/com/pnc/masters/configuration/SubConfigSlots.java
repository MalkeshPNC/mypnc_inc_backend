package com.pnc.masters.configuration;

import java.util.ArrayList;
import java.util.List;

final class SubConfigSlots {

    static final int MAX = 8;

    private SubConfigSlots() {
    }

    static List<String> labelsOf(SubConfigType type) {
        List<String> labels = new ArrayList<>();
        for (int index = 1; index <= MAX; index++) {
            String label = type.getFieldLabel(index);
            if (label == null || label.isBlank()) {
                break;
            }
            labels.add(label);
        }
        return labels;
    }

    static List<String> valuesOf(SubConfigEntry entry, int fieldCount) {
        List<String> values = new ArrayList<>();
        for (int index = 1; index <= fieldCount; index++) {
            values.add(entry.getFieldValue(index));
        }
        return values;
    }

    static void applyLabels(SubConfigType type, List<String> labels) {
        for (int index = 1; index <= MAX; index++) {
            type.setFieldLabel(index, index <= labels.size() ? labels.get(index - 1) : null);
        }
    }

    static void applyValues(SubConfigEntry entry, List<String> values) {
        for (int index = 1; index <= MAX; index++) {
            entry.setFieldValue(index, index <= values.size() ? values.get(index - 1) : null);
        }
    }
}
