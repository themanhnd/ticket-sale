package com.ticketsale.gateway.filter;

import com.ticketsale.common.util.CorrelationIdUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

//Giải thích CorrelationIdFilter
//Nếu request đã có X-Correlation-Id, giữ lại.
//Nếu chưa có, tự sinh UUID mới.
//Gắn header này vào request đi tiếp.
//Gắn header này vào response trả về client.

// Gắn correlation id vào response để client và log có thể đối chiếu request.
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final String HEADER_NAME = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestCorrelationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
        String correlationId = (requestCorrelationId == null || requestCorrelationId.isBlank())
                ? CorrelationIdUtil.newCorrelationId()
                : requestCorrelationId;

        exchange.getResponse().getHeaders().set(HEADER_NAME, correlationId);
        return chain.filter(exchange);
    }
//    Vì sao getOrder() trả -100?
//    Filter order càng nhỏ thì chạy càng sớm.
//    Correlation ID nên được gắn sớm để các filter/service sau dùng được.
    @Override
    public int getOrder() {
        return -100;
    }
}