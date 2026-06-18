package de.htwberlin.webtech.recipe.instructions;

import de.htwberlin.webtech.recipe.dto.InstructionSearchRequest;
import de.htwberlin.webtech.recipe.dto.InstructionSearchResponse;
import de.htwberlin.webtech.recipe.dto.InstructionSearchResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class InstructionSearchService {

    private static final Logger LOG = Logger.getLogger(InstructionSearchService.class);

    private final InstructionSearchClient client;

    public InstructionSearchService(InstructionSearchClient client) {
        this.client = client;
    }

    public InstructionSearchResponse search(InstructionSearchRequest request) {
        String title = normalize(request.getRecipeTitle());
        String sourceName = normalize(request.getSourceName());

        try {
            List<InstructionSearchResult> results = queries(title, sourceName).stream()
                    .flatMap(query -> client.search(query).stream())
                    .collect(
                            LinkedHashMap<String, InstructionSearchResult>::new,
                            (byUrl, result) -> byUrl.putIfAbsent(result.getUrl(), result),
                            Map::putAll
                    )
                    .values()
                    .stream()
                    .limit(5)
                    .toList();

            InstructionSearchResponse response = new InstructionSearchResponse();
            response.setConfigured(true);
            response.setResults(results);
            response.setGoogleSearchUrl(googleSearchUrl(title));
            if (results.isEmpty()) {
                response.setMessage("Keine Online-Treffer gefunden.");
            }
            return response;
        } catch (InstructionSearchNotConfiguredException e) {
            InstructionSearchResponse response = new InstructionSearchResponse();
            response.setConfigured(false);
            response.setMessage("Online-Suche ist aktuell nicht konfiguriert.");
            response.setGoogleSearchUrl(googleSearchUrl(title));
            response.setResults(List.of());
            return response;
        } catch (InstructionSearchClientException e) {
            LOG.warnf("Instruction web search failed for recipe '%s': %s", title, e.getMessage());
            InstructionSearchResponse response = new InstructionSearchResponse();
            response.setConfigured(true);
            response.setMessage("Online-Suche konnte aktuell nicht durchgeführt werden.");
            response.setGoogleSearchUrl(googleSearchUrl(title));
            response.setResults(List.of());
            return response;
        }
    }

    private List<String> queries(String title, String sourceName) {
        List<String> values = new ArrayList<>();
        values.add("\"" + title + "\" recipe instructions");
        values.add("\"" + title + "\" Zubereitung Rezept");
        if (!sourceName.isBlank()) {
            values.add("\"" + title + "\" " + sourceName);
        }
        return values;
    }

    private String googleSearchUrl(String title) {
        return "https://www.google.com/search?q="
                + URLEncoder.encode(title + " recipe instructions", StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
