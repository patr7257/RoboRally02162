package dk.dtu.config;

import dk.dtu.config.RestTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration; // Import this
import org.springframework.web.cors.CorsConfigurationSource; // Import this
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Import this

import java.util.Arrays;
import java.util.List;

/**
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RestTokenFilter restTokenFilter;

    public SecurityConfig(RestTokenFilter restTokenFilter) {
        this.restTokenFilter = restTokenFilter;
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. ENABLE CORS HERE (Crucial change!)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/client/**","/host/**").permitAll()
                        .requestMatchers("/api/users/create", "/api/users/login").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(restTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*","https://se2-f.compute.dtu.dk"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}