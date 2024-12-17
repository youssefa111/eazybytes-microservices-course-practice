package com.eazybytes.gatewayserver.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {


    @RequestMapping("/contact-support")
    public Mono<String> contactSupport(){
        return Mono.just("An Error Occurred, Please Try Again Later or contact support team.");
    }
}
