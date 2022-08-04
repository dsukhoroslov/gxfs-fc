package eu.gaiax.difs.fc.core.pojo;

/**
 * Accessor class for passing SelfDescription content. Implementations may use
 * lazy-loading to improve memory use.
 */
public interface SelfDescriptionContent {

    /**
     * Returns the JSON-LD content of the Self-Description as a string.
     *
     * @return the JSON-LD content of the Self-Description as a string.
     */
    public String getSelfDescriptionContent();
}
