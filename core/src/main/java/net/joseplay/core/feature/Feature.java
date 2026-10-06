package net.joseplay.core.feature;

public interface Feature<T> {

    String getId();

    String getType();

    Class<T> getValueType();

    T getDefaultValue();

    String serialize(T value);

    T deserialize(String value);

    default void validate(T value) {
    }

    default T add(T current, T amount) {return null;}

    default T cast(Object value) {
        return getValueType().cast(value);
    }
}
