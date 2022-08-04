package eu.gaiax.difs.fc.core.pojo;

/**
 * A direct string implementation of the SelfDescriptionContent interface.
 */
public class SelfDescriptionContentDirect implements SelfDescriptionContent {

    private final String content;

    public SelfDescriptionContentDirect(String content) {
        this.content = content;
    }

    @Override
    public String getSelfDescriptionContent() {
        return content;
    }

}
