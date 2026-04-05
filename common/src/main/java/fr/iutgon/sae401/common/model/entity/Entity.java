package fr.iutgon.sae401.common.model.entity;

import fr.iutgon.sae401.common.json.Json;

public abstract class Entity {
    protected String id;
    protected Position position;

    public Entity(String id, Position position) {
        this.id = id;
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public abstract Json toJson();
}