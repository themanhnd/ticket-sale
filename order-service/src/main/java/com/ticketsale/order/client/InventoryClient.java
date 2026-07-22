package com.ticketsale.order.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

// Client gọi sang inventory-service để giữ vé.
@Component
public class InventoryClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryClient(
            RestClient.Builder restClientBuilder,
            @Value("${inventory.service.base-url}") String inventoryBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(inventoryBaseUrl)
                .build();
    }

    // Giữ quantity vé của eventId trước khi order được lưu.
    public void reserve(Long eventId, Integer quantity) {
        try {
            restClient.post()
                    .uri("/api/inventories/{eventId}/reserve", eventId)
                    .body(new ReserveInventoryRequest(quantity))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new IllegalArgumentException(extractMessage(exception), exception);
        }
    }

    private String extractMessage(RestClientResponseException exception) {
        try {
            JsonNode root = objectMapper.readTree(exception.getResponseBodyAsString());
            String message = root.path("message").asText();
            if (!message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            // Nếu body không phải JSON thì trả message gốc của HTTP client.
        }
        return exception.getMessage();
    }

    private record ReserveInventoryRequest(Integer quantity) {
    }
}