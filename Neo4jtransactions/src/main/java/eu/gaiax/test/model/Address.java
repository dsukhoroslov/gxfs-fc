package eu.gaiax.test.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name = "Address")
public class Address {

  @Id
  @Column(name = "id")
  private Long id;
  @Column(name = "name")
  private String name;

  public Address(){

  }
  public Address (Long id ,String name){
    this.id = id;
    this.name = name;
  }
}
