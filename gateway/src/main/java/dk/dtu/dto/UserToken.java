package dk.dtu.dto;

import java.util.Date;

public record UserToken(String userID, Date issueDate, Date expiryDate){ }
