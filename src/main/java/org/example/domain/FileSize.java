package org.example.domain;

public enum FileSize {

    SMALL,
    MEDIUM,
    LARGE,
    EXTRA_LARGE;

    private final String value;

    FileSize() {
        this.value = this.name();
    }

    @Override
    public String toString() {
        return "FileSize{" +
                "value='" + value + '\'' +
                '}';
    }
}
