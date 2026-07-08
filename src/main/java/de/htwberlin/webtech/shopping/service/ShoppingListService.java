package de.htwberlin.webtech.shopping.service;

import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.shopping.dto.ShoppingListItemRequest;
import de.htwberlin.webtech.shopping.entity.ShoppingListItem;
import de.htwberlin.webtech.shopping.exception.ShoppingListItemNotFoundException;
import de.htwberlin.webtech.shopping.mapper.ShoppingListItemMapper;
import de.htwberlin.webtech.shopping.repository.ShoppingListItemRepository;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * Owner-scoped CRUD for a user's manually managed shopping list items. For the list
 * auto-generated from the weekly meal plan, see {@link de.htwberlin.webtech.mealplan.service.MealPlanShoppingListService}.
 */
@ApplicationScoped
public class ShoppingListService {

    private final ShoppingListItemRepository repo;
    private final ShoppingListItemMapper mapper;

    public ShoppingListService(ShoppingListItemRepository repo, ShoppingListItemMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<ShoppingListItem> listMine(AppUser currentUser) {
        return repo.findByOwner(currentUser);
    }

    @Transactional
    public ShoppingListItem create(ShoppingListItemRequest request, AppUser currentUser) {
        ShoppingListItem item = mapper.toEntity(request);
        item.setOwner(currentUser);
        repo.persist(item);
        return item;
    }

    @Transactional
    public ShoppingListItem update(Long id, ShoppingListItemRequest request, AppUser currentUser) {
        ShoppingListItem existing = findExisting(id);
        ensureOwner(existing, currentUser);
        mapper.updateEntity(existing, request);
        return existing;
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        ShoppingListItem existing = findExisting(id);
        ensureOwner(existing, currentUser);
        repo.delete(existing);
    }

    private ShoppingListItem findExisting(Long id) {
        ShoppingListItem item = repo.findById(id);
        if (item == null) {
            throw new ShoppingListItemNotFoundException(id);
        }
        return item;
    }

    private void ensureOwner(ShoppingListItem item, AppUser currentUser) {
        if (item.getOwner() == null) {
            throw new ForbiddenException("Only the shopping list item owner may access this shopping list item.");
        }
        if (currentUser == null || currentUser.getId() == null || !currentUser.getId().equals(item.getOwner().getId())) {
            throw new ForbiddenException("Only the shopping list item owner may access this shopping list item.");
        }
    }
}
