package fr.iutgon.sae401.serverSide.server.clients;

/**
 * Registry for managing player nicknames.
 */
public interface NicknameRegistry {
    /**
     * Set a nickname for a client.
     */
    void setNickname(ClientId id, String nickname);

    /**
     * Get the nickname for a client.
     */
    String getNickname(ClientId id);

    /**
     * Remove a client's nickname.
     */
    void remove(ClientId id);
}
