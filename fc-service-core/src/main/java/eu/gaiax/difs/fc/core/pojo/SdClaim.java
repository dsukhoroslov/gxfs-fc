package eu.gaiax.difs.fc.core.pojo;

/**
 * POJO Class for holding a Signature.
 */
public class SdClaim {
    public final String subject;
    public final String predicate;
    public final String object;


    public SdClaim(String subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    public String getSubject() {
        return subject;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }


}
