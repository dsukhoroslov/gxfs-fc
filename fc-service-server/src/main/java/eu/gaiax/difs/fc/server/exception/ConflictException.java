package eu.gaiax.difs.fc.server.exception;

/**
 * ConflictException is an exception that can be thrown to customize the KeyCloak conflicts error of the Federated
 * Catalogue server application.
 * Implementation of the {@link ServiceException} exception.
 */
public class ConflictException extends ServiceException {
  /**
   * Constructs a new Conflict Exception with the specified detail message.
   *
   * @param message Detailed message about the thrown exception.
   */
  public ConflictException(String message) {
    super(message);
  }
}
