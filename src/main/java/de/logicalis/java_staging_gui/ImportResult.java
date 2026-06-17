package de.logicalis.java_staging_gui;

import java.util.List;

public record ImportResult(List<String> imported, List<String> errors) {

    public int total() {
        return imported.size() + errors.size();
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
