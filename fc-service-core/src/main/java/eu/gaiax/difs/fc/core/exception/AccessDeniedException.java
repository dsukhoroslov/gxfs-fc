package eu.gaiax.difs.fc.core.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * ClientException is an exception that can be thrown to customize the Bad request error of the Federated
 * Catalogue server application.
 * Implementation of the {@link ServiceException} exception.
 */
@Getter
@Setter
public class AccessDeniedException extends ServiceException {
  /**
   * Constructs a new Client Exception with the specified detail message.
   *
   * @param message Detailed message about the thrown exception.
   */
  public AccessDeniedException(String message) {
    super(message);
  }
}