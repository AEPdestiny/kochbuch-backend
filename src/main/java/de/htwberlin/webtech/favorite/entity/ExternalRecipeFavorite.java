package de.htwberlin.webtech.favorite.entity;

import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_external_recipe_favorite_owner_source_id", columnNames = {"owner_id", "external_source", "external_recipe_id"})
})
public class ExternalRecipeFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "external_recipe_id", nullable = false, length = 80)
    private String externalRecipeId;

    @Column(name = "external_title", nullable = false, length = 255)
    private String externalTitle;

    @Column(name = "external_image_url", length = 1000)
    private String externalImageUrl;

    @Column(name = "external_source", nullable = false, length = 40)
    private String externalSource = "SPOONACULAR";

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (externalSource == null || externalSource.isBlank()) {
            externalSource = "SPOONACULAR";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getOwner() {
        return owner;
    }

    public void setOwner(AppUser owner) {
        this.owner = owner;
    }

    public String getExternalRecipeId() {
        return externalRecipeId;
    }

    public void setExternalRecipeId(String externalRecipeId) {
        this.externalRecipeId = externalRecipeId;
    }

    public String getExternalTitle() {
        return externalTitle;
    }

    public void setExternalTitle(String externalTitle) {
        this.externalTitle = externalTitle;
    }

    public String getExternalImageUrl() {
        return externalImageUrl;
    }

    public void setExternalImageUrl(String externalImageUrl) {
        this.externalImageUrl = externalImageUrl;
    }

    public String getExternalSource() {
        return externalSource;
    }

    public void setExternalSource(String externalSource) {
        this.externalSource = externalSource;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
