package eu.gaiax.test.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import eu.gaiax.test.model.Address;

@Repository
public interface AddressRepositoryPostgres extends CrudRepository<Address,Long> {
}
