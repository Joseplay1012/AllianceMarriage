package net.joseplay.core.feature.defaults;

import net.joseplay.core.feature.Feature;

public class PvPFeature implements Feature<Boolean> {
    @Override
    public String getId() {
        return "pvp";
    }

    @Override
    public String getType() {
        return "BOOLEAN";
    }

    @Override
    public Class<Boolean> getValueType() {
        return Boolean.class;
    }

    @Override
    public Boolean getDefaultValue() {
        return true;
    }

    @Override
    public String serialize(Boolean value) {
        return value.toString();
    }

    @Override
    public Boolean deserialize(String value) {
        return Boolean.valueOf(value);
    }
}
