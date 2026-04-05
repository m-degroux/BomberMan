package fr.iutgon.sae401.serverSide.game.input;

import fr.iutgon.sae401.common.json.Json;
import fr.iutgon.sae401.serverSide.server.clients.ClientId;

/**
 * @brief One input event emitted by a client.
 */
public record InputEvent(ClientId clientId, Json payload) {
}
