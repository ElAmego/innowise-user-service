package com.innowise.userservice;

import com.innowise.userservice.configuration.AppConfiguration;
import com.innowise.userservice.configuration.WebConfig;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(AppConfiguration.class);

        servletContext.addListener(new ContextLoaderListener(rootContext));

        FilterRegistration.Dynamic securityFilter = servletContext.addFilter(
                "springSecurityFilterChain",
                new DelegatingFilterProxy("springSecurityFilterChain")
        );
        securityFilter.addMappingForUrlPatterns(null, false, "/*");
        securityFilter.setAsyncSupported(true);

        AnnotationConfigWebApplicationContext servletContextConfig = new AnnotationConfigWebApplicationContext();
        servletContextConfig.register(WebConfig.class);

        DispatcherServlet dispatcherServlet = new DispatcherServlet(servletContextConfig);
        jakarta.servlet.ServletRegistration.Dynamic dispatcher = servletContext.addServlet(
                "dispatcher",
                dispatcherServlet
        );
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
    }
}