package eu.gaiax.test.repository;

import eu.gaiax.test.model.ServiceMap;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface MapDataRepository extends Neo4jRepository<ServiceMap, String> {
}
