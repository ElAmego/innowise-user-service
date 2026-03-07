package com.innowise.userservice;

import com.innowise.userservice.configuration.AppConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Main {
    static void main() {
        try (final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfiguration.class)) {
            System.out.println("Spring application just has run.");
        }
    }
}