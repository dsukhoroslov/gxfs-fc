package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;


/**
 * POJO Class for holding the validators, that signed the Self-Description.
 */
@lombok.Getter
@lombok.Setter
public class Validator {
    String didURI;

    String publicKey;

    Instant expirationDaten;
}
