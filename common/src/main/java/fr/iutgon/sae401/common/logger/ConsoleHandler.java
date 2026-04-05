package fr.iutgon.sae401.common.logger;

public class ConsoleHandler extends LogHandler {

    public ConsoleHandler() {
        super(LogLevel.INFO);
    }

    @Override
    protected void write(LogMessage msg) {
        System.out.println(msg.format());
    }
}
