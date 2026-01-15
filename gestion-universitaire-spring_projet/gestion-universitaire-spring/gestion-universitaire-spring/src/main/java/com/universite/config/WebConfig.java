package com.universite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Pages publiques
        registry.addViewController("/").setViewName("redirect:/login");
        registry.addViewController("/login").setViewName("auth/login");
        registry.addViewController("/access-denied").setViewName("error/access-denied");

        // Pages de test
        registry.addViewController("/test").setViewName("test");
        registry.addViewController("/simple-test").setViewName("simple-test");
        registry.addViewController("/test-dashboard").setViewName("test-dashboard");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}