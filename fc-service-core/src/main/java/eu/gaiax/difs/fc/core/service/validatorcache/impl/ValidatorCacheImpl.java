package eu.gaiax.difs.fc.core.service.validatorcache.impl;

import eu.gaiax.difs.fc.core.pojo.Validator;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import eu.gaiax.difs.fc.core.service.validatorcache.ValidatorCache;
import org.springframework.transaction.annotation.Transactional;

/**
 * A cache for Validator information that is loaded from a DID.
 *
 * @author hylke
 */
@Slf4j
@Component
@Transactional
public class ValidatorCacheImpl implements ValidatorCache {

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public Validator getFromCache(String didURI) {
    final Session currentSession = sessionFactory.getCurrentSession();
    final Validator validator = currentSession.find(Validator.class, didURI);
    if (validator != null) {
      currentSession.detach(validator);
    }
    return validator;
  }

  @Override
  public void addToCache(Validator validator) {
    final Session currentSession = sessionFactory.getCurrentSession();
    currentSession.persist(validator);
    currentSession.flush();
    currentSession.detach(validator);
  }

  @Override
  public void removeFromCache(String didURI) {
    String deleteQuery = "delete from Validator where didURI = :uri";
    sessionFactory.getCurrentSession().createMutationQuery(deleteQuery)
        .setParameter("uri", didURI)
        .executeUpdate();
  }

  /**
   * Removed expired validators from the cache.
   *
   * @return the number if deleted validators.
   */
  @Override
  public int expireValidators() {
    final Session currentSession = sessionFactory.getCurrentSession();
    String deleteQuery = "delete from Validator where expirationDate <= :dateTime";
    int deleted = currentSession.createMutationQuery(deleteQuery)
        .setParameter("dateTime", Instant.now())
        .executeUpdate();
    log.debug("Expired {} Validators", deleted);
    return deleted;
  }

}
