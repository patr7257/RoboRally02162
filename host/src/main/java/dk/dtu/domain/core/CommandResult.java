package dk.dtu.domain.core;

// Author(s) Weihao Mo

public record CommandResult(boolean ok, String reason) {
    public static CommandResult ok(String r) {
        return new CommandResult(true, r);
    }
    public static CommandResult fail(String r) {
        return new CommandResult(false, r);
    }
}


