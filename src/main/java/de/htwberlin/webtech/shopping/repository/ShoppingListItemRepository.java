package de.htwberlin.webtech.shopping.repository;

import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ShoppingListItemRepository implements PanacheRepository<ShoppingListItem> {

    public List<ShoppingListItem> findByOwner(AppUser owner) {
        return list("owner", owner);
    }
}
