package net.joseplay.test.feature.defaults;

import net.joseplay.test.feature.Feature;

public class TextFeature implements Feature<String> {
    @Override
    public String getId() {
        return "text";
    }

    @Override
    public String getType() {
        return "STRING";
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }

    @Override
    public String getDefaultValue() {
        return "isso é o inicio";
    }

    @Override
    public String serialize(String value) {
        return value;
    }

    @Override
    public String deserialize(String value) {
        return value;
    }
}
