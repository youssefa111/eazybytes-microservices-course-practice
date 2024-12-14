package com.eazybytes.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayserverApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayserverApplication.class, args);
	}

	@Bean
	public RouteLocator eazyBankRouteConfig(RouteLocatorBuilder routeLocatorBuilder){
		return routeLocatorBuilder.routes()
				.route(p -> p
						.path("/eazybank/accounts/**")
						.filters(f -> f.rewritePath("eazybank/accounts/(?<segment>.*)","/${segment}"))
						.uri("lb://ACCOUNT"))
				.route(p -> p
						.path("/eazybank/loans/**")
						.filters(f -> f.rewritePath("eazybank/loans/(?<segment>.*)","/${segment}"))
						.uri("lb://LOANS"))
				.route(p -> p
						.path("/eazybank/cards/**")
						.filters(f -> f.rewritePath("eazybank/cards/(?<segment>.*)","/${segment}"))
						.uri("lb://CARDS")).build();
	}

}
