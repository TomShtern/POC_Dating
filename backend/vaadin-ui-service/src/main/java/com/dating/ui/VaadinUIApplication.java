package com.dating.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Vaadin UI Service - Main Application Class
 *
 * Pure Java web UI for POC Dating Application
 * - Vaadin 24.3 for UI components
 * - Spring Boot 3.2.0 for backend integration
 * - Feign clients for microservice communication
 * - Redis for session management
 */
@SpringBootApplication
@EnableFeignClients
@Theme(value = "dating-theme", variant = Lumo.DARK)
public class VaadinUIApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(VaadinUIApplication.class, args);
    }
}
