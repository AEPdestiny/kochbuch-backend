package de.htwberlin.webtech.pantry.repository;

import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class PantryItemRepository implements PanacheRepository<PantryItem> {

    public List<PantryItem> findByOwner(AppUser owner) {
        return list("owner", owner);
    }
}
