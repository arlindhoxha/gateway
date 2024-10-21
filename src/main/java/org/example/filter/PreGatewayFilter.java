package org.example.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

/**
 * Created by Arlind Hoxha on 10/20/2024.
 */
@Component
public class PreGatewayFilter implements GlobalFilter, Ordered {
    private static final String AUTHORIZATION = "Authorization";

    @Value("${spring.security.oauth2.client.clientId}")
    private String clientId;

    @Value("${spring.security.oauth2.client.clientSecret}")
    private String clientSecret;

    private final List<HttpMessageReader<?>> messageReaders = getMessageReaders();

    private List<HttpMessageReader<?>> getMessageReaders() { return HandlerStrategies.withDefaults().messageReaders(); }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        if (exchange.getRequest().getPath().toString().contains("oauth/token")) {
            return filterWithEncodedAuthorizationHeader(exchange, chain);
        } else if (headers.containsKey(AUTHORIZATION)) {
            exchange.getRequest().mutate().header(AUTHORIZATION, headers.getFirst(AUTHORIZATION));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    public Mono<Void> filterWithEncodedAuthorizationHeader(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (exchange.getRequest().getQueryParams().containsKey("password")) {
            ServerWebExchange exchange1 = exchange.mutate().request(getModifyRequest(exchange)).build();
            return chain.filter(exchange1);
        } else {
            return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
                DataBufferUtils.retain(dataBuffer);
                Flux<DataBuffer> cachedFlux = Flux.defer(() -> Flux.just(dataBuffer.slice(0, dataBuffer.readableByteCount())));
                ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return cachedFlux;
                    }
                };
                return ServerRequest.create(exchange.mutate().request(mutatedRequest).build(), messageReaders)
                        .bodyToMono(String.class)
                        .flatMap(body -> {
                            if (!body.contains("client_credentials")) {
                                getModifyRequest(exchange);
                            }
                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        });
            });
        }
    }

    private ServerHttpRequest getModifyRequest(ServerWebExchange exchange) {
        String auth = clientId + ":" + clientSecret;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);
        ServerHttpRequest request = exchange.getRequest().mutate().header(AUTHORIZATION, authHeader).build();
        return request;
    }
}
