package de.htwberlin.webtech.recipe.resource;

import de.htwberlin.webtech.recipe.dto.ImageUploadResponse;
import de.htwberlin.webtech.recipe.image.SupabaseStorageService;
import de.htwberlin.webtech.security.UserContext;
import de.htwberlin.webtech.user.entity.AppUser;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Path("/recipes/images")
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public class RecipeImageResource {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Set<String> ALLOWED_CONTENT_TYPES = EXTENSIONS_BY_CONTENT_TYPE.keySet();

    private final UserContext userContext;
    private final SupabaseStorageService storageService;

    public RecipeImageResource(UserContext userContext, SupabaseStorageService storageService) {
        this.userContext = userContext;
        this.storageService = storageService;
    }

    @POST
    public ImageUploadResponse uploadImage(
            @HeaderParam("Authorization") String authorizationHeader,
            @RestForm("file") FileUpload file
    ) {
        AppUser currentUser = userContext.requireUser(authorizationHeader);
        validate(file);

        String contentType = file.contentType();
        String extension = EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
        String objectPath = "recipes/" + currentUser.getId() + "/" + UUID.randomUUID() + "." + extension;
        String imageUrl = storageService.upload(file.uploadedFile(), objectPath, contentType);

        return new ImageUploadResponse(imageUrl);
    }

    private void validate(FileUpload file) {
        if (file == null || file.uploadedFile() == null) {
            throw new IllegalArgumentException("Image file is required.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.contentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are supported.");
        }
        if (file.size() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file must not exceed 5 MB.");
        }
    }
}
