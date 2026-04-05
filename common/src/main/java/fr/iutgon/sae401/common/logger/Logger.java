package fr.iutgon.sae401.common.logger;

public class Logger {
    private static Logger logger;
    private LogHandler handler;

    private Logger() {
        handler = new ConsoleHandler();
        handler.setNext(new FileHandler());
    }

    public static Logger getLogger() {
        if (logger == null) logger = new Logger();
        return logger;
    }

    public void log(LogMessage msg) {
        handler.handle(msg);
    }
}
