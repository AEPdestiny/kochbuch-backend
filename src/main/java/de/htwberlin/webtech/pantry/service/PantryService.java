package de.htwberlin.webtech.pantry.service;

import de.htwberlin.webtech.pantry.dto.PantryItemRequest;
import de.htwberlin.webtech.pantry.entity.PantryItem;
import de.htwberlin.webtech.pantry.exception.PantryItemNotFoundException;
import de.htwberlin.webtech.pantry.mapper.PantryItemMapper;
import de.htwberlin.webtech.pantry.repository.PantryItemRepository;
import de.htwberlin.webtech.shared.exception.ForbiddenException;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;

/** Owner-scoped CRUD for a user's pantry items (list/create/update/delete). */
@ApplicationScoped
public class PantryService {

    private final PantryItemRepository repo;
    private final PantryItemMapper mapper;

    public PantryService(PantryItemRepository repo, PantryItemMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public List<PantryItem> listMine(AppUser currentUser) {
        return repo.findByOwner(currentUser);
    }

    @Transactional
    public PantryItem create(PantryItemRequest request, AppUser currentUser) {
        PantryItem item = mapper.toEntity(request);
        item.setOwner(currentUser);
        repo.persist(item);
        return item;
    }

    @Transactional
    public PantryItem update(Long id, PantryItemRequest request, AppUser currentUser) {
        PantryItem existing = findExisting(id);
        ensureOwner(existing, currentUser);
        mapper.updateEntity(existing, request);
        return existing;
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        PantryItem existing = findExisting(id);
        ensureOwner(existing, currentUser);
        repo.delete(existing);
    }

    private PantryItem findExisting(Long id) {
        PantryItem item = repo.findById(id);
        if (item == null) {
            throw new PantryItemNotFoundException(id);
        }
        return item;
    }

    private void ensureOwner(PantryItem item, AppUser currentUser) {
        if (item.getOwner() == null) {
            throw new ForbiddenException("Only the pantry item owner may access this pantry item.");
        }
        if (currentUser == null || currentUser.getId() == null || !currentUser.getId().equals(item.getOwner().getId())) {
            throw new ForbiddenException("Only the pantry item owner may access this pantry item.");
        }
    }
}
