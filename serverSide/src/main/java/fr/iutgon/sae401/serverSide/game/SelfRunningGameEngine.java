package fr.iutgon.sae401.serverSide.game;

/**
 * Marker interface for engines that manage their own internal loop(s).
 * <p>
 * When an engine implements this interface, the outer server runtime should not
 * schedule {@link GameEngine#tick(double)} / {@link GameEngine#netTick()}.
 */
public interface SelfRunningGameEngine {
}
