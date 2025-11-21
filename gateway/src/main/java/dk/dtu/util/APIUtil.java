package dk.dtu.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class APIUtil {
    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     * @return the userID (string) of the caller of the API, extracted from userToken header.
     */
    public static  String getCallerID() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return  (String) auth.getPrincipal();
    }
}
