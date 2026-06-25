package de.htwberlin.webtech.recipe.instructions;

import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;
import de.htwberlin.webtech.recipe.dto.RecipeInstructionSuggestion;
import de.htwberlin.webtech.recipe.dto.RecipeInstructionSuggestionResponse;
import de.htwberlin.webtech.recipe.entity.Recipe;
import de.htwberlin.webtech.recipe.service.RecipeInstructionNormalizer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class RecipeInstructionSuggestionService {

    private static final Logger LOG = Logger.getLogger(RecipeInstructionSuggestionService.class);

    private final InstructionSearchClient client;

    public RecipeInstructionSuggestionService(InstructionSearchClient client) {
        this.client = client;
    }

    public RecipeInstructionSuggestionResponse suggestFor(Recipe recipe) {
        RecipeInstructionSuggestionResponse response = baseResponse(recipe);
        List<String> realSteps = RecipeInstructionNormalizer.normalizeToList(
                recipe.getInstructions(),
                recipe.getTitle(),
                recipe.getCategory(),
                recipe.getDishTypes(),
                List.of(),
                recipe.getLanguage()
        );
        response.setHasRealInstructions(!realSteps.isEmpty());
        if (!realSteps.isEmpty()) {
            response.setConfigured(true);
            response.setMessage("Dieses Rezept hat bereits eine Zubereitung.");
            response.setSuggestions(List.of());
            return response;
        }

        try {
            List<RecipeInstructionSuggestion> suggestions = queries(recipe).stream()
                    .flatMap(query -> client.search(query).stream())
                    .collect(
                            LinkedHashMap<String, InstructionSearchResult>::new,
                            (byUrl, result) -> byUrl.putIfAbsent(normalize(result.getUrl()), result),
                            Map::putAll
                    )
                    .values()
                    .stream()
                    .map(result -> toSuggestion(result, recipe))
                    .filter(suggestion -> !suggestion.getSteps().isEmpty())
                    .limit(5)
                    .toList();

            response.setConfigured(true);
            response.setSuggestions(suggestions);
            if (suggestions.isEmpty()) {
                response.setMessage("Keine belastbaren Zubereitungsvorschläge gefunden.");
            }
            return response;
        } catch (InstructionSearchNotConfiguredException e) {
            response.setConfigured(false);
            response.setMessage("Online-Suche ist aktuell nicht konfiguriert.");
            response.setSuggestions(List.of());
            return response;
        } catch (InstructionSearchClientException e) {
            LOG.warnf("Instruction suggestion search failed for recipe %s: %s", recipe.getId(), e.getMessage());
            response.setConfigured(true);
            response.setMessage("Zubereitungsvorschläge konnten aktuell nicht geladen werden.");
            response.setSuggestions(List.of());
            return response;
        }
    }

    private RecipeInstructionSuggestionResponse baseResponse(Recipe recipe) {
        RecipeInstructionSuggestionResponse response = new RecipeInstructionSuggestionResponse();
        response.setRecipeId(recipe.getId());
        response.setConfigured(true);
        response.setSuggestions(List.of());
        return response;
    }

    private List<String> queries(Recipe recipe) {
        String title = normalize(recipe.getTitle());
        List<String> values = new ArrayList<>();
        values.add("\"" + title + "\" recipe instructions steps");
        values.add("\"" + title + "\" Zubereitung Rezept Schritte");

        String sourceName = normalize(recipe.getSourceName());
        if (!sourceName.isBlank()) {
            values.add("\"" + title + "\" \"" + sourceName + "\" instructions");
        }

        String domain = domain(recipe.getSourceUrl());
        if (!domain.isBlank()) {
            values.add("site:" + domain + " \"" + title + "\" recipe instructions");
        }
        return values;
    }

    private RecipeInstructionSuggestion toSuggestion(InstructionSearchResult result, Recipe recipe) {
        List<String> steps = RecipeInstructionNormalizer.extractRealStepsFromSnippet(result.getSnippet());
        return new RecipeInstructionSuggestion(
                result.getTitle(),
                result.getUrl(),
                steps,
                confidence(result, recipe),
                "Aus Websuche abgeleitete Vorschlagsquelle. Bitte vor dem Kochen prüfen."
        );
    }

    private double confidence(InstructionSearchResult result, Recipe recipe) {
        String sourceDomain = domain(recipe.getSourceUrl());
        String resultDomain = domain(result.getUrl());
        if (!sourceDomain.isBlank() && sourceDomain.equals(resultDomain)) {
            return 0.85;
        }

        String title = normalize(recipe.getTitle()).toLowerCase(Locale.ROOT);
        String haystack = (normalize(result.getTitle()) + " " + normalize(result.getSnippet())).toLowerCase(Locale.ROOT);
        if (!title.isBlank() && haystack.contains(title)) {
            return 0.7;
        }
        return 0.55;
    }

    private String domain(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            String host = URI.create(value).getHost();
            if (host == null) {
                return "";
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
