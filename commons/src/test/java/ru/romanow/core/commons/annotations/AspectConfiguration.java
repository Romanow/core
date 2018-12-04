package ru.romanow.core.commons.annotations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

@Profile("aspect-test")
@Configuration
@EnableAspectJAutoProxy
class AspectConfiguration {

    @Bean
    public LogRequestAspect logRequestAspect() {
        return new LogRequestAspect();
    }

    @Bean
    public AspectTestClass aspectTestClass() {
        return new AspectTestClass();
    }
}
