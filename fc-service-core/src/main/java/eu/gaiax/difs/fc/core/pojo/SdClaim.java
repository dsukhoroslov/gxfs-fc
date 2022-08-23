package eu.gaiax.difs.fc.core.pojo;

/**
 * POJO Class for holding a Claim.
 */
@lombok.AllArgsConstructor
@lombok.EqualsAndHashCode
@lombok.Getter
public class SdClaim {

  private final String subject;
  private final String predicate;
  private final String object;
  @Override
  public String toString()
  {
    return "(" +subject + " ," + predicate + " ," + object +")";
  }



}
