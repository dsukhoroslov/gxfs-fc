package eu.gaiax.difs.fc.core.exception;

/**
 * This exception is thrown whenever a claim will be passed to the graph
 * store that does not match the credential subjects it describes.
 */
public class ClaimCredentialSubjectException extends RuntimeException {
    public ClaimCredentialSubjectException(String msg) {
        super(msg);
    }
}
