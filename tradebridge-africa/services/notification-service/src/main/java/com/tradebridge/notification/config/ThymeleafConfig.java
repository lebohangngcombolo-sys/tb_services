package com.tradebridge.notification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Reserved for future HTML email rendering via Thymeleaf templates under
 * {@code templates/emails/}. Not currently wired into the delivery
 * pipeline — {@link com.tradebridge.notification.template.TemplateRenderer}
 * handles the `${var}` placeholders used by seeded templates.
 */
@Configuration
public class ThymeleafConfig {

    @Bean
    public SpringTemplateEngine emailTemplateEngine(SpringResourceTemplateResolver emailResolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(emailResolver);
        return engine;
    }

    @Bean
    public SpringResourceTemplateResolver emailResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/emails/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        resolver.setOrder(1);
        return resolver;
    }
}
