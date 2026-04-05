package fr.iutgon.sae401.clientSide.network;

import fr.iutgon.sae401.common.protocol.MessageEnvelope;

public interface NetworkObserver {
	public static final NetworkManager networkManager = NetworkManager.getManager();
	void onMessage(MessageEnvelope message);
}
