package com.example.demo.acp;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "acp")
public record AcpProperties(String command, List<String> arguments, String workingDirectory, boolean autoApprovePermissions) {

    public AcpProperties {
        if (command == null || command.isBlank()) {
            command = "qwen";
        }
        if (arguments == null || arguments.isEmpty()) {
            arguments = List.of("--acp");
        }
        if (workingDirectory == null || workingDirectory.isBlank()) {
            workingDirectory = System.getProperty("user.home");
        }
    }

    /**
     * Runs the agent through a zsh login-less shell that sources ~/.zshrc first, so that
     * environment variables set up there (PATH additions, API keys, nvm, etc.) are visible
     * to the agent process even though it's spawned directly by the JVM.
     */
    public List<String> processCommand() {
        StringBuilder script = new StringBuilder("source ~/.zshrc; exec ").append(shellQuote(command));
        for (String arg : arguments) {
            script.append(' ').append(shellQuote(arg));
        }
        return List.of("/bin/zsh", "-c", script.toString());
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
