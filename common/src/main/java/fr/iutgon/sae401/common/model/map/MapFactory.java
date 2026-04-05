package fr.iutgon.sae401.common.model.map;

/**
 * Interface de fabrique pour créer des GameMap.
 */
public interface MapFactory {

	/**
	 * Crée une carte selon un thème.
	 *
	 * @param width  largeur
	 * @param height hauteur
	 * @param theme  thème de génération
	 * @return carte générée
	 */
	GameMap create(int width, int height, MapTheme theme);

	/**
	 * Compatibilité: crée une carte avec le thème par défaut.
	 */
	default GameMap create(int width, int height) {
		return create(width, height, MapTheme.CLASSIC);
	}
}