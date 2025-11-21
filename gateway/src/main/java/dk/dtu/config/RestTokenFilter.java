package dk.dtu.config;

import dk.dtu.dto.UserToken;
import dk.dtu.shared.AuthManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
/**
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */
@Component
public class RestTokenFilter extends OncePerRequestFilter {
    private final AuthManager authManager;

    public RestTokenFilter(AuthManager authManager) {
        this.authManager = authManager;
    }
    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/client") || path.startsWith("/host") || path.equals("/api/users/create") || path.equals("/api/users/login") || request.getMethod().equalsIgnoreCase("OPTIONS"); //always allow "permission" checks.
    }
    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (authManager.validateToken(token)) {
                UserToken userToken = authManager.extractUserToken(token);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userToken.userID(), null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }
        }
        // If we get here, they are unauthorized
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing Token");
    }
}