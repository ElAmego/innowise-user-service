package com.innowise.userservice;

import com.innowise.userservice.configuration.AppConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class WebAppInitializer implements WebApplicationInitializer {
    private static final String DISPATCHER = "dispatcher";

    @Override
    public void onStartup(ServletContext servletContext) {
        final AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(AppConfiguration.class);

        final DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        final ServletRegistration.Dynamic registration = servletContext.addServlet(DISPATCHER, dispatcherServlet);

        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}