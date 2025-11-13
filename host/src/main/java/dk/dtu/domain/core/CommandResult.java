package dk.dtu.domain.core;

/**
 * Result of executing a game command, indicating success or failure with a reason.
 *
 * @param ok {@code true} if the command executed successfully
 * @param reason a message explaining the result (success message or error details)
 * @author Weihao Mo
 */
public record CommandResult(boolean ok, String reason) {
    public static CommandResult ok(String r) {
        return new CommandResult(true, r);
    }
    public static CommandResult fail(String r) {
        return new CommandResult(false, r);
    }
}