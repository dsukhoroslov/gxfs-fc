package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;

/**
 * POJO Class for holding the validators, that signed the Self-Description.
 */
@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class Validator {
    private String didURI;

    private String publicKey;

    private Instant expirationDate;
}
