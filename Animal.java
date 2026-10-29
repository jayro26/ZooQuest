package main;

public absrtact class Animal {

  protected final String name;
  protected final String species;
  protected final String habitat;
  protected final String diet;
  protected final String status;
  protected final String behaviour
}

protected Animal (String name, String species, String habitat, String diet, String status, String behaviour) {

 this.name = name;
 this.species = species;
 this.habitat = habitat;
 this.diet = diet;
 this.status = status;
 this.behaviour = behaviour;
}

public static Animal create(String name, String species, String habitat, String diet, String status, String behaviour) {

  switch(origin) {
    case 'J' return new Jungle(name, species, habitat, diet, status, behaviour);
    case 'S' return new Safari(name, species, habitat, diet, status, behaviour);
    case 'A' return new Aquatic(name, species, habitat, diet, status, behaviour);
      
         
  }
  
}

