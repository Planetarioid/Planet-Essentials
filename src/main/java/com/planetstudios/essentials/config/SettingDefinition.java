package com.planetstudios.essentials.config;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** A typed, validated setting owned by Planet Essentials. */
public final class SettingDefinition<T> {
    public enum Kind { BOOLEAN, INTEGER }

    private final String id, category, displayName, description;
    private final Kind kind;
    private final T defaultValue;
    private final Predicate<T> validator;
    private final Function<EssentialsConfig, T> reader;
    private final BiConsumer<EssentialsConfig, T> writer;

    public SettingDefinition(String id, String category, String displayName, String description,
                             Kind kind, T defaultValue, Predicate<T> validator,
                             Function<EssentialsConfig, T> reader,
                             BiConsumer<EssentialsConfig, T> writer) {
        this.id = requireText(id, "id");
        this.category = requireText(category, "category");
        this.displayName = requireText(displayName, "displayName");
        this.description = Objects.requireNonNull(description, "description");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.defaultValue = Objects.requireNonNull(defaultValue, "defaultValue");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.reader = Objects.requireNonNull(reader, "reader");
        this.writer = Objects.requireNonNull(writer, "writer");
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " cannot be blank");
        return value;
    }

    public String id() { return id; }
    public String category() { return category; }
    public String displayName() { return displayName; }
    public String description() { return description; }
    public Kind kind() { return kind; }
    public T defaultValue() { return defaultValue; }
    public T get(EssentialsConfig config) { return reader.apply(config); }

    public boolean isValid(Object value) {
        try {
            @SuppressWarnings("unchecked") T typed = (T) value;
            return value != null && validator.test(typed);
        } catch (ClassCastException exception) {
            return false;
        }
    }

    public void set(EssentialsConfig config, Object value) {
        if (!isValid(value)) throw new IllegalArgumentException("Invalid value or type for setting " + id);
        @SuppressWarnings("unchecked") T typed = (T) value;
        writer.accept(config, typed);
    }
}
