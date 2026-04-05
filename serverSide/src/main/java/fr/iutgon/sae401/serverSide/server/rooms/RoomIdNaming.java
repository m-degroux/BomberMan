package fr.iutgon.sae401.serverSide.server.rooms;

import java.util.Objects;

/**
 * Small helper to build stable room/match ids from user-provided names.
 */
public final class RoomIdNaming {
	private RoomIdNaming() {
	}

	public static String baseNameFrom(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.trim();
		if (s.startsWith("room-")) {
			s = s.substring("room-".length());
		} else if (s.startsWith("match-")) {
			s = s.substring("match-".length());
		}
		// Normalize whitespace to hyphen; keep common safe characters.
		s = s.replaceAll("\\s+", "-");
		s = s.replaceAll("[^a-zA-Z0-9_-]", "");
		return s;
	}

	public static RoomId roomIdFromBaseName(String baseName) {
		String base = Objects.requireNonNull(baseName, "baseName").trim();
		if (base.isEmpty()) {
			throw new IllegalArgumentException("baseName cannot be empty");
		}
		return new RoomId("room-" + base);
	}

	public static RoomId matchIdFromBaseName(String baseName) {
		String base = Objects.requireNonNull(baseName, "baseName").trim();
		if (base.isEmpty()) {
			throw new IllegalArgumentException("baseName cannot be empty");
		}
		return new RoomId("match-" + base);
	}
}
