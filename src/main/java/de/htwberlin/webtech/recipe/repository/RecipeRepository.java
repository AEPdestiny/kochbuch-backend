package de.htwberlin.webtech.recipe.repository;

import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.user.entity.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RecipeRepository implements PanacheRepository<Recipe> {

    public List<Recipe> findPublished() {
        return list("published", true);
    }

    public List<Recipe> findRandomPublished(int limit) {
        return getEntityManager()
                .createQuery("""
                        from Recipe r
                        where r.published = true
                        order by function('random')
                        """, Recipe.class)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Recipe> findRandomPublishedByLanguage(String language, int limit) {
        return getEntityManager()
                .createQuery("""
                        from Recipe r
                        where r.published = true
                          and lower(coalesce(r.language, 'en')) = :language
                        order by function('random')
                        """, Recipe.class)
                .setParameter("language", normalizeLanguage(language))
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Recipe> findRandomPublishedByLanguageAndCategory(String language, String category, int limit) {
        return getEntityManager()
                .createQuery("""
                        from Recipe r
                        where r.published = true
                          and lower(coalesce(r.language, 'en')) = :language
                          and lower(coalesce(r.category, '')) = :category
                        order by function('random')
                        """, Recipe.class)
                .setParameter("language", normalizeLanguage(language))
                .setParameter("category", category == null ? "" : category.trim().toLowerCase())
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Recipe> findByOwner(AppUser owner) {
        return list("owner", owner);
    }

    public Optional<Recipe> findByTitleAndCategory(String title, String category) {
        return find("lower(title) = ?1 and lower(category) = ?2", title.toLowerCase(), category.toLowerCase())
                .firstResultOptional();
    }

    public Optional<Recipe> findByTitleAndCategoryAndLanguage(String title, String category, String language) {
        return find("lower(title) = ?1 and lower(category) = ?2 and lower(coalesce(language, 'en')) = ?3",
                title.toLowerCase(),
                category.toLowerCase(),
                normalizeLanguage(language)
        ).firstResultOptional();
    }

    public Optional<Recipe> findSeedByExternalIdCategoryAndLanguage(String externalId, String category, String language) {
        if (externalId == null || externalId.isBlank()) {
            return Optional.empty();
        }
        return find("""
                owner is null
                and externalId = ?1
                and lower(category) = ?2
                and lower(coalesce(language, 'en')) = ?3
                """,
                externalId.trim(),
                category.toLowerCase(),
                normalizeLanguage(language)
        ).firstResultOptional();
    }

    public Optional<Recipe> findSeedByTitleCategoryAndLanguage(String title, String category, String language) {
        return find("""
                owner is null
                and lower(title) = ?1
                and lower(category) = ?2
                and lower(coalesce(language, 'en')) = ?3
                """,
                title.toLowerCase(),
                category.toLowerCase(),
                normalizeLanguage(language)
        ).firstResultOptional();
    }

    private String normalizeLanguage(String language) {
        return language == null || language.isBlank() ? "en" : language.trim().toLowerCase();
    }
}
