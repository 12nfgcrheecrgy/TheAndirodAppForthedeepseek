package com.deepseek.chat.domain.model;

public class ModelInfo {

    private final String id;
    private final String ownedBy;
    private final boolean supportsReasoning;

    public ModelInfo(String id, String ownedBy, boolean supportsReasoning) {
        this.id = id;
        this.ownedBy = ownedBy;
        this.supportsReasoning = supportsReasoning;
    }

    public String getId() {
        return id;
    }

    public String getOwnedBy() {
        return ownedBy;
    }

    public boolean isSupportsReasoning() {
        return supportsReasoning;
    }

    @Override
    public String toString() {
        return id;
    }
}
