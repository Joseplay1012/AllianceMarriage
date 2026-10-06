package net.joseplay.test.storage;

import net.joseplay.test.couple.Couple;

import java.util.Optional;
import java.util.UUID;

public interface CouplesRepository {
    Couple create(UUID partner1, UUID partner2) throws Exception;

    Optional<Couple> find(UUID partner1, UUID partner2);

    Optional<Couple> findById(UUID id);


}
