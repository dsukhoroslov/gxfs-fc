package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;

/**
 * POJO Class for holding the validators, that signed the Self-Description.
 */
@lombok.Getter
public class Validator {
    private String didURI;

    private String publicKey;

    private Instant expirationDaten;

    private boolean changed = false;

    public Validator (String didURI, String publicKey, Instant expirationDaten) {
        this.didURI = didURI;
        this.publicKey = publicKey;
        this.expirationDaten = expirationDaten;
    }

    public void setDidURi (String didURI) {
        this.didURI = didURI;
        changed = true;
    }

    public void setPublicKey (String publicKey) {
        this.publicKey = publicKey;
        changed = true;
    }

    public void setExpirationDate (Instant expirationDaten) {
        this.expirationDaten = expirationDaten;
        changed = true;
    }
}
