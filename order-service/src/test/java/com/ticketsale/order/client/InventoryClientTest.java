package com.ticketsale.order.client;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class InventoryClientTest {

    @Test
    void reserveShouldCallInventoryReserveEndpoint() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();
        InventoryClient inventoryClient = new InventoryClient(
                restClientBuilder,
                "http://inventory-service:8093"
        );

        server.expect(requestTo("http://inventory-service:8093/api/inventories/1001/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        inventoryClient.reserve(1001L, 2);

        server.verify();
    }

    @Test
    void reserveShouldThrowBusinessMessageWhenInventoryRejectsRequest() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();
        InventoryClient inventoryClient = new InventoryClient(
                restClientBuilder,
                "http://inventory-service:8093"
        );

        server.expect(requestTo("http://inventory-service:8093/api/inventories/1001/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"message\":\"Không đủ vé để giữ chỗ\",\"data\":null}"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventoryClient.reserve(1001L, 2)
        );

        assertEquals("Không đủ vé để giữ chỗ", exception.getMessage());
        server.verify();
    }
}