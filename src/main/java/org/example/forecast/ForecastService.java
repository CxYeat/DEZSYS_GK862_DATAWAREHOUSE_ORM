package org.example.forecast;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.warehouse.ProductPurchaseEntity;
import org.example.warehouse.ProductPurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ForecastService {

    private final ProductPurchaseRepository purchaseRepository;
    private final RestTemplate restTemplate;
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public ForecastService(ProductPurchaseRepository purchaseRepository, RestTemplate restTemplate) {
        this.purchaseRepository = purchaseRepository;
        this.restTemplate = restTemplate;
    }

    public String generateForecast() {
        List<ProductPurchaseEntity> purchases = purchaseRepository.findAll();

        if (purchases.isEmpty()) {
            return "No sales data available to generate a forecast.";
        }

        // Aggregate data for the prompt
        Map<String, Integer> salesByProduct = purchases.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getProduct().getName(),
                        Collectors.summingInt(ProductPurchaseEntity::getAmount)
                ));

        Map<String, Integer> salesByWarehouse = purchases.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getWarehouse().getName(),
                        Collectors.summingInt(ProductPurchaseEntity::getAmount)
                ));

        StringBuilder prompt = new StringBuilder();
        prompt.append("Based on the following historical sales data from my data warehouse, ");
        prompt.append("generate a forecast for the coming 3 months for each product and warehouse.\n\n");
        
        prompt.append("Sales by Product:\n");
        salesByProduct.forEach((name, amount) -> prompt.append("- ").append(name).append(": ").append(amount).append("\n"));
        
        prompt.append("\nSales by Warehouse:\n");
        salesByWarehouse.forEach((name, amount) -> prompt.append("- ").append(name).append(": ").append(amount).append("\n"));

        prompt.append("\nPlease provide a detailed trend analysis and estimated sales numbers for each.");

        return callOllama(prompt.toString());
    }

    private String callOllama(String prompt) {
        try {
            OllamaRequest request = new OllamaRequest("qwen3", prompt, false);
            OllamaResponse response = restTemplate.postForObject(OLLAMA_URL, request, OllamaResponse.class);
            
            if (response != null && response.getResponse() != null) {
                return response.getResponse();
            }
            return "Failed to get a response from Ollama.";
        } catch (Exception e) {
            return "Error calling Ollama: " + e.getMessage() + ". Make sure Ollama is running on localhost:11434 with model qwen2.5.";
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class OllamaRequest {
        private String model;
        private String prompt;
        private boolean stream;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class OllamaResponse {
        private String response;
    }
}
