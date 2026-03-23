package org.example.forecast;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.warehouse.ProductPurchaseEntity;
import org.example.warehouse.ProductPurchaseRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ForecastService {

    private final ProductPurchaseRepository purchaseRepository;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    public ForecastService(ProductPurchaseRepository purchaseRepository, RestTemplate restTemplate) {
        this.purchaseRepository = purchaseRepository;
        this.restTemplate = restTemplate;
    }

    public String generateForecast() {
        List<ProductPurchaseEntity> purchases = purchaseRepository.findAll();

        if (purchases.isEmpty()) {
            return "No sales data available to generate a forecast.";
        }

        // Datenaggregation (deine Logik bleibt gleich)
        String salesSummary = purchases.stream()
                .collect(Collectors.groupingBy(p -> p.getProduct().getName(), Collectors.summingInt(ProductPurchaseEntity::getAmount)))
                .entrySet().stream()
                .map(e -> "- " + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));

        String prompt = "Generate a 3-month sales forecast based on this data:\n" + salesSummary;

        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        try {
            // Google Gemini erwartet ein "contents" Array mit "parts"
            GeminiRequest request = new GeminiRequest(
                    Collections.singletonList(new Content(
                            Collections.singletonList(new Part(prompt))
                    ))
            );

            // API Key wird als Query-Parameter angehängt
            String urlWithKey = apiUrl + "?key=" + apiKey;

            GeminiResponse response = restTemplate.postForObject(urlWithKey, request, GeminiResponse.class);

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                return response.getCandidates().get(0).getContent().getParts().get(0).getText();
            }
            return "Failed to get a response from Gemini.";
        } catch (Exception e) {
            return "Error calling Gemini: " + e.getMessage();
        }
    }

    // --- Hilfsklassen für das Google API Format ---

    @Data @AllArgsConstructor @NoArgsConstructor
    static class GeminiRequest {
        private List<Content> contents;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    static class Content {
        private List<Part> parts;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    static class Part {
        private String text;
    }

    @Data @NoArgsConstructor
    static class GeminiResponse {
        private List<Candidate> candidates;

        @Data @NoArgsConstructor
        static class Candidate {
            private Content content;
        }
    }
}