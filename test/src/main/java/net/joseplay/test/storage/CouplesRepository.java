package net.joseplay.core.storage;

import net.joseplay.core.couple.Couple;

import java.util.Optional;
import java.util.UUID;

public interface CouplesRepository {
    Couple create(UUID partner1, UUID partner2) throws Exception;

    Optional<Couple> find(UUID partner1, UUID partner2);


}
