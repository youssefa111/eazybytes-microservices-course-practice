package com.eazybytes.gatewayserver;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	/*
	 This config of custom route of microservices by gateway service
	 - we customized APIs URL of microservices to be in lower case
	 - we add circuitBreaker with help of circuitbreaker-reactor-resilience4j Dependency to Account microservice with fallbackUri
	 - we add retryPattern to Loans microservice for GET methods with 3 retries with some config
	 - we add rate limiter with help of redis-reactive Dependency to Cards microservice to limit requests sent by user to prevent any spam or overload on the server
	*/
	@Bean
	public RouteLocator eazyBankRouteConfig(RouteLocatorBuilder routeLocatorBuilder){
		return routeLocatorBuilder.routes()
				.route(p -> p
						.path("/eazybank/accounts/**")
						.filters(f -> f.rewritePath("eazybank/accounts/(?<segment>.*)","/${segment}")
								.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								// Here we add CircuitBreaker to Accounts microservice
								.circuitBreaker(config -> config.setName("AccountCircuitBreaker").setFallbackUri("forward:/contact-support"))
						)
						.uri("lb://ACCOUNT"))

				.route(p -> p
						.path("/eazybank/loans/**")
						.filters(f -> f.rewritePath("eazybank/loans/(?<segment>.*)","/${segment}")
								.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								// Here we add Retry Pattern  to Loans microservice's GET method APIS
								.retry(retryConfig ->
										retryConfig
										.setRetries(3)
										.setMethods(HttpMethod.GET)
										.setBackoff(Duration.ofMillis(100),Duration.ofMillis(1000),2,true)
								)
						)
						.uri("lb://LOANS"))

				.route(p -> p
						.path("/eazybank/cards/**")
						.filters(f -> f.rewritePath("eazybank/cards/(?<segment>.*)","/${segment}")
								.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								// Here we add Rate Limiter to Cards microservice's APIS
								.requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter()).setKeyResolver(userKeyResolver()) )
						)
						.uri("lb://CARDS"))

				.build();
	}


	// This Config of timeout of Resilience of api call before the counter of circuit breaker increase to  open state
	@Bean
	public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
		return factory->factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
						.circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
				.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(4)).build()).build());
	}

	// This config of rate limiter for the system
	@Bean
	public RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(1, 1, 1);
	}
	@Bean
	KeyResolver userKeyResolver() {
		return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("user"))
				.defaultIfEmpty("anonymous");
	}

}
