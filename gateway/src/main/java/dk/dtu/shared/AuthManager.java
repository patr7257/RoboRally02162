package dk.dtu.shared;

import dk.dtu.dto.UserToken;
import dk.dtu.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * @author Karl Johannes Agerbo
 * @author Niklas Emil Lysdal
 */
@Service
public class AuthManager {
    private final TokenUtil tokenUtil;
    private final int maxUserTokenAge = 1000*60*60*24;
    @Autowired
    public AuthManager(TokenUtil tokenUtil) {
        this.tokenUtil = tokenUtil;
    }

    /**
     * @author Karl Johannes Agerbo
     * @author Niklas Emil Lysdal
     */
    public String getUserToken(String userID) {
        return tokenUtil.generateUserToken(userID,maxUserTokenAge);
    }

    public boolean validateToken(String token) {
        return tokenUtil.validateToken(token);
    }



    public UserToken extractUserToken(String token) {
        return tokenUtil.extractUserToken(token);
    }

    public int getMaxUserTokenAge() {
        return maxUserTokenAge;
    }


}
