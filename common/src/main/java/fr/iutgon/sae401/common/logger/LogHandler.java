package fr.iutgon.sae401.common.logger;

public abstract class LogHandler {
    protected LogHandler next;
    protected LogLevel minLevel;

    public LogHandler(LogLevel minLevel) {
        this.minLevel = minLevel;
    }

    public void setNext(LogHandler next) {
        this.next = next;
    }

    public void handle(LogMessage msg) {
        if (minLevel.compareTo(msg.getLevel()) <= 0)
            write(msg);
        if (next != null)
            next.handle(msg);
    }

    protected abstract void write(LogMessage msg);
}
