package eu.gaiax.test.repository;

import java.util.List;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Component;

import eu.gaiax.test.model.Person;

@Component
public interface PersonRepository extends Neo4jRepository<Person, Long> {

	Person findByName(String name);
	List<Person> findByTeammatesName(String name);
}
