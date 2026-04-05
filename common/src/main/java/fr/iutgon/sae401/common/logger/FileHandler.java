package fr.iutgon.sae401.common.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileHandler extends LogHandler {

    private File file = new File("src/main/resources/fr/iutgon/sae401/logs/app.log");

    public FileHandler() {
        super(LogLevel.WARNING);
    }

    @Override
    protected void write(LogMessage msg) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (var bw = new BufferedWriter(new FileWriter(file, true))) {
            String line = msg.format().replaceAll("\\033\\[[\\d;]*m", "");
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            System.err.println("Erreur FileHandler : " + e.getMessage());
        }
    }

}
