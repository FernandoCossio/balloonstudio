package com.decoraciones.config;

import com.decoraciones.common.decorators.CurrentUserIdResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final CurrentUserIdResolver currentUserIdResolver;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir;
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + location);
    }


	public WebConfig(CurrentUserIdResolver currentUserIdResolver) {
		this.currentUserIdResolver = currentUserIdResolver;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(currentUserIdResolver);
	}
}
