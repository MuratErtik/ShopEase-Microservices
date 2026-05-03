package org.n11bootcamp.paymentservice.configs;



import org.n11bootcamp.paymentservice.enums.TargetSystem;
import org.n11bootcamp.paymentservice.outboxes.OutboxEventHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class OutboxHandlerConfig {

    @Bean
    public Map<TargetSystem, OutboxEventHandler> outboxHandlers(List<OutboxEventHandler> handlerList) {
        return handlerList.stream()
                .collect(Collectors.toMap(OutboxEventHandler::getTargetSystem, handler -> handler));
    }
}
