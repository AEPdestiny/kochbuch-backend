package de.htwberlin.webtech.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class GroqClient {

    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://api.groq.com/openai/v1/chat/completions");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String model;

    @Inject
    public GroqClient(ObjectMapper objectMapper,
                      @ConfigProperty(name = "groq.api.key") Optional<String> apiKey,
                      @ConfigProperty(name = "groq.model", defaultValue = "llama-3.3-70b-versatile") String model) {
        this(objectMapper, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build(), apiKey.orElse(""), model);
    }

    GroqClient(ObjectMapper objectMapper, HttpClient httpClient, String apiKey, String model) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    public String complete(String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new GroqClientException("GROQ_API_KEY is not configured.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", 700);
        ArrayNode messages = payload.putArray("messages");
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userMessage));

        try {
            HttpRequest request = HttpRequest.newBuilder(CHAT_COMPLETIONS_URI)
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new GroqClientException("Groq request failed with status " + response.statusCode() + ".");
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new GroqClientException("Groq response did not contain a message.");
            }
            return content.trim();
        } catch (IOException exception) {
            throw new GroqClientException("Groq request failed.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GroqClientException("Groq request was interrupted.", exception);
        }
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }
}
