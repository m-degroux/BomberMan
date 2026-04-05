package fr.iutgon.sae401.common.logger;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogMessage {
    private String content;
    private LogLevel level;
    private LocalTime timestamp;

    public LogMessage(String content, LogLevel level) {
        this.content = content;
        this.level = level;
        this.timestamp = LocalTime.now();
    }

    public String format() {
        String color;
        switch (level) {
            case INFO:
                color = "\033[34m";
                break;
            case WARNING:
                color = "\033[38;5;136m";
                break;
            default:
                color = "\033[31m";
                break;
        }
        return String.format("%s[%s] [%s] - %s\033[0m", color, timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")), level, content);
    }

    public LogLevel getLevel() {
        return level;
    }
}
