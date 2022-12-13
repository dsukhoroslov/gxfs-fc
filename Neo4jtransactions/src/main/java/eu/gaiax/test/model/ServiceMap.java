package eu.gaiax.test.model;

import java.util.Map;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class ServiceMap {

  @Id
  private String id;

  @CompositeProperty
  Map<String, Object> mapData;

  public ServiceMap(String id, Map<String, Object> mapData) {
    this.id = id;
    this.mapData = mapData;
  }
}
