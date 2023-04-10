package io.github.barmoury.api.config;

import io.github.barmoury.eloquent.QueryArmoury;
import io.github.barmoury.eloquent.sqlinterface.MySqlInterface;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WebSecurityConfig implements WebMvcConfigurer {

    @Autowired
    Environment environment;

    @Autowired
    EntityManager entityManager;

    @Autowired(required = false)
    JwtRequestFilter jwtRequestFilter;

    @Autowired(required = false)
    RouteValidatorFilter routeValidatorFilter;

    List<CorsMapping> corsMappings = new ArrayList<>();

    @Autowired
    AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((request, response, ex) -> response.sendError(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        ex.getMessage()))
                .and()
                .authorizeHttpRequests();
        if (jwtRequestFilter != null) {
            for (Map.Entry<String, List<String>> entry : jwtRequestFilter.getOpenUrlMatchers().entrySet()) {
                for (String path : entry.getValue()) {
                    HttpMethod httpMethod = HttpMethod.valueOf(entry.getKey());
                    if (entry.getKey().equalsIgnoreCase("ANY")) {
                        http.authorizeHttpRequests().requestMatchers(path).permitAll();
                    } else {
                        http.authorizeHttpRequests().requestMatchers(httpMethod, path).permitAll();
                    }
                }
            }
        }
        http.authorizeHttpRequests().anyRequest().permitAll();

        if (routeValidatorFilter != null) http.addFilterBefore(routeValidatorFilter, UsernamePasswordAuthenticationFilter.class);
        if (jwtRequestFilter != null) http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    public void registerCorsMappings(CorsMapping corsMapping) {
        corsMappings.add(corsMapping);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (corsMappings.isEmpty()) corsMappings.add(new CorsMapping());
        for (CorsMapping corsMapping : corsMappings) {
            registry.addMapping(corsMapping.pattern)
                    .allowCredentials(corsMapping.allowCredentials)
                    .allowedOrigins(corsMapping.origins)
                    .allowedMethods(corsMapping.methods)
                    .allowedHeaders(corsMapping.headers);
        }
    }

    @Bean
    QueryArmoury mySqlEloquentInterface() {
        QueryArmoury  queryArmoury = new QueryArmoury(new MySqlInterface());
        queryArmoury.setEntityManager(entityManager);
        queryArmoury.setAutowireCapableBeanFactory(autowireCapableBeanFactory);
        String namingStrategy = environment.getProperty("spring.jackson.property-naming-strategy");
        if (namingStrategy != null) queryArmoury.setSnakeCase(namingStrategy.equalsIgnoreCase("SNAKE_CASE"));
        return queryArmoury;
    }

    public static class CorsMapping {
        boolean allowCredentials = false;
        String pattern = "/**";
        String[] headers = new String[]{"*"};
        String[] origins = new String[]{"*"};
        String[] methods = new String[]{"*"};

        public CorsMapping pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        public CorsMapping allowCredentials(boolean allowCredentials) {
            this.allowCredentials = allowCredentials;
            return this;
        }

        public CorsMapping headers(String[] headers) {
            this.headers = headers;
            return this;
        }

        public CorsMapping origins(String[] origins) {
            this.origins = origins;
            return this;
        }

        public CorsMapping methods(String[] methods) {
            this.methods = methods;
            return this;
        }

    }

}
