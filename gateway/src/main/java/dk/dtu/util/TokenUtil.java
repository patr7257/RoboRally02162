package dk.dtu.util;

import dk.dtu.dto.UserToken;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
/**
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */

@Service
public class TokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    public TokenUtil() {}
    public TokenUtil(String secret) {
        this.secret = secret;
    }
    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    public  String generateUserToken(String userID,int maxTokenAge) {
        return Jwts.builder()
                .setSubject(userID)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+maxTokenAge))
                .signWith(getSigningKey())
                .compact();

    }
  


    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().
                    setSigningKey(getSigningKey()).
                    parseClaimsJws(token);
            return true;

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
           // System.out.println("Invalid JWT signature.");
        } catch (ExpiredJwtException e) {
            //System.out.println("Expired JWT token.");
        } catch (UnsupportedJwtException e) {
            //System.out.println("Unsupported JWT token.");
        } catch (IllegalArgumentException e) {
            //System.out.println("JWT claims string is empty.");
        }
        return false;
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    public UserToken extractUserToken(String userToken)  {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(userToken).getBody();
        return new UserToken(claims.getSubject(),claims.getIssuedAt(),claims.getExpiration());
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

}
