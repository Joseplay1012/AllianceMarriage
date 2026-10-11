package net.joseplay.api.preferences;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PreferencesManager {
    private final Set<Preference> preferences = ConcurrentHashMap.newKeySet();


    public void register(Preference preference){
        preferences.add(preference);
    }


    public Set<Preference> getAll(){
        return Collections.unmodifiableSet(preferences);
    }


}
