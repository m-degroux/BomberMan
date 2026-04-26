package fr.iutgon.sae401.serverSide.server.clients;

/**
 * Registry for managing player skins.
 * Ensures that only one player per skin is allowed.
 */
public interface SkinRegistry {
    /**
     * Set a skin ID for a client.
     * Returns true if the skin was successfully assigned, false if already taken.
     */
    boolean setSkin(ClientId id, int skinId);

    /**
     * Get the skin ID for a client.
     */
    int getSkin(ClientId id);

    /**
     * Check if a skin is already taken by another client.
     */
    boolean isSkinTaken(int skinId, ClientId currentClient);

    /**
     * Remove a client's skin assignment.
     */
    void remove(ClientId id);

    /**
     * Get the client ID that owns a specific skin, or null if not taken.
     */
    ClientId getOwner(int skinId);
}
