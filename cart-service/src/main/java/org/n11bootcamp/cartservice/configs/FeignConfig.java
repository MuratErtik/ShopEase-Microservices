package org.n11bootcamp.cartservice.configs;



import feign.codec.ErrorDecoder;
import org.n11bootcamp.cartservice.exceptions.FeignErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
