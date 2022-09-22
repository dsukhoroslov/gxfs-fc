package eu.gaiax.difs.fc.core.pojo;

import java.util.Objects;

/**
 * POJO Class for holding a Claim. A Claim is a triple represented by a subject, predicate, and object.
 */
@lombok.AllArgsConstructor
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.Setter
public class SdClaim {

  private final String subject;
  private final String predicate;
  private final String object;
  @Override
  public boolean equals(Object o){
    if (this == o)
      return true;
    // null check
    if (o == null)
      return false;
    // type check and cast
    if (getClass() != o.getClass())
      return false;
    SdClaim person = (SdClaim) o;
    // field comparison
    return Objects.equals(subject, person.subject)
            && Objects.equals(predicate, person.predicate)
            && Objects.equals(object, person.object);
  }
}
