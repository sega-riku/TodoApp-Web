package com.sega.todoappweb.contact;

public enum ContactType {

    QUESTION("質問"),

    OPINION("意見・要望");

    private final String displayName;

    ContactType(
        String displayName
    ) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}