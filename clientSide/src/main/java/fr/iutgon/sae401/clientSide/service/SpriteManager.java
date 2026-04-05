package fr.iutgon.sae401.clientSide.service;

import javafx.scene.image.Image;

import java.util.HashMap;
import java.util.Map;

public class SpriteManager {
    private static final Map<String, Image> cache = new HashMap<>();
    private static final String BASE_PATH = "/fr/iutgon/sae401/sprites/output/";

    public static Image get(String category, String name) {
        String path = BASE_PATH + category + "/" + name;
        return cache.computeIfAbsent(path, p -> {
            try {
                var stream = SpriteManager.class.getResourceAsStream(p);
                return (stream != null) ? new Image(stream) : null;
            } catch (Exception e) {
                System.err.println("Erreur de chargement asset : " + p);
                return null;
            }
        });
    }

    public static Image getPlayerFrame(int skinId, String dir, int frame) {
        return get("characters/" + skinId, dir + "_" + frame + ".png");
    }

    public static Image getPlayerFrame(int skinId, String animation) {
        return get("characters/" + skinId, animation + ".png");
    }

    public static Image getBombFrame(int frame) {
        return get("bomb", "B2_" + frame + ".png");
    }
}