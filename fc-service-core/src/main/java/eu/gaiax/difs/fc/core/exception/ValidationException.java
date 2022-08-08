package eu.gaiax.difs.fc.core.exception;

/**
 * ValidationException is an exception that can be thrown when the validation of an SD fails
 * Implementation of the {@link ServiceException} exception.
 */
public class ValidationException extends ServiceException {
  /**
   * Constructs a new Client Exception with the specified detail message.
   *
   * @param message Detailed message about the thrown exception.
   */
  public ValidationException(String message) {
    super(message);
  }
}