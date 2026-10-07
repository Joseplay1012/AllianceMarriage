package net.joseplay.core.feature.defaults;

import net.joseplay.core.feature.Feature;

public class HeartsFeature implements Feature<Integer> {
    @Override
    public String getId() {
        return "hearts";
    }

    @Override
    public String getType() {
        return "INTEGER";
    }

    @Override
    public Class<Integer> getValueType() {
        return Integer.class;
    }

    @Override
    public Integer getDefaultValue() {
        return 0;
    }

    @Override
    public String serialize(Integer value) {
        return Integer.toString(value);
    }

    @Override
    public Integer deserialize(String value) {
        return Integer.parseInt(value);
    }

    @Override
    public void validate(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Hearts cannot be null"
            );
        }
    }
}
