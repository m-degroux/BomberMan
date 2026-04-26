package fr.iutgon.sae401.serverSide.server.clients;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of SkinRegistry.
 */
public class InMemorySkinRegistry implements SkinRegistry {
    private static final int DEFAULT_SKIN = 0;
    private final ConcurrentMap<ClientId, Integer> skinByClient = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, ClientId> clientBySkin = new ConcurrentHashMap<>();

    @Override
    public boolean setSkin(ClientId id, int skinId) {
        if (id == null) {
            return false;
        }

        // Check if this skin is already taken by another client
        ClientId currentOwner = clientBySkin.get(skinId);
        if (currentOwner != null && !currentOwner.equals(id)) {
            return false;
        }

        // Remove old skin assignment if the client had one
        Integer oldSkinId = skinByClient.put(id, skinId);
        if (oldSkinId != null && !oldSkinId.equals(skinId)) {
            clientBySkin.remove(oldSkinId, id);
        }

        // Assign new skin
        clientBySkin.put(skinId, id);
        return true;
    }

    @Override
    public int getSkin(ClientId id) {
        if (id == null) {
            return DEFAULT_SKIN;
        }
        return skinByClient.getOrDefault(id, DEFAULT_SKIN);
    }

    @Override
    public boolean isSkinTaken(int skinId, ClientId currentClient) {
        ClientId owner = clientBySkin.get(skinId);
        return owner != null && !owner.equals(currentClient);
    }

    @Override
    public void remove(ClientId id) {
        if (id == null) {
            return;
        }
        Integer skinId = skinByClient.remove(id);
        if (skinId != null) {
            clientBySkin.remove(skinId, id);
        }
    }

    @Override
    public ClientId getOwner(int skinId) {
        return clientBySkin.get(skinId);
    }
}
