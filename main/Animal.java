package main;

import java.util.ArrayList; // NEW CODE
import java.util.List; // NEW CODE

public abstract class Animal {

  private final String name;
  private final String species;
  private final String habitat;
  private final String diet;
  private final String status;
  private final String behaviour;
  private final String uniqueFeature;
  private MysteryClue associatedClue; 

protected Animal(String name, String species, String habitat, String diet, String status, String behaviour, String uniqueFeature) {
  
  this.name = name;
  this.species = species;
  this.habitat = habitat;
  this.diet = diet;
  this.status = status;
  this.behaviour = behaviour;
  this.uniqueFeature = uniqueFeature;
}

public static Animal create(char type, String name, String species, String habitat, String diet, String status, String behaviour, String feature) {

  switch (type) {
    case 'J':
      return new Jungle(name, species, habitat, diet, status, behaviour, feature);
    case 'S':
      return new Safari(name, species, habitat, diet, status, behaviour, feature);
    case 'A':
      return new Aquatic(name, species, habitat, diet, status, behaviour, feature);
    default: // added code
      throw new IllegalArgumentException("Unknown animal type: " + type); // added code
  }
}

// NEW CODE: each animal type owns its animals (see Jungle, Safari, Aquatic).
public static List<Animal> createAnimalsFor(char type, String biomeName) {
  switch (type) {
    case 'J':
      return Jungle.createAnimals(biomeName);
    case 'S':
      return Safari.createAnimals(biomeName);
    case 'A':
      return Aquatic.createAnimals(biomeName);
    default:
      throw new IllegalArgumentException("Unknown animal type: " + type);
  }
}

// NEW CODE: shared by Jungle, Safari and Aquatic. Turns their data rows into animals + clues.
// Row format: { name, species, diet, status, behaviour, uniqueFeature (description), clueHint }
static List<Animal> fromRows(char type, String biomeName, String[][] rows) {
  List<Animal> animals = new ArrayList<>();
  for (int i = 0; i < rows.length; i++) {
    String[] row = rows[i];
    if (row.length != 7) {
      throw new IllegalStateException("Animal " + (i + 1) + " of " + biomeName + " needs 7 fields but has " + row.length);
    }
    Animal animal = create(type, row[0], row[1], biomeName, row[2], row[3], row[4], row[5]); // habitat = biome name
    animal.setAssociatedClue(new MysteryClue(biomeName + "_clue_" + (i + 1), row[6], row[0]));
    animals.add(animal);
  }
  return animals;
}

public abstract String getAnimalType();
public abstract String getMovementStyle();
public abstract String getSpriteTemplate();
public abstract String getRevealMessage(); // NEW CODE: each type reacts to being found in its own way

public String displayBehaviour() {
  return behaviour + "\n\n[" + getAnimalType() + "] " + getMovementStyle();
}

public String getDescription() {
  return name + " (" + species + ") \nUnique feature: " + uniqueFeature;}

public MysteryClue getClue() {
  return associatedClue;
}

public void setAssociatedClue(MysteryClue c) {
  // NEW CODE: the clue can be attached only once
  if (c == null) throw new IllegalArgumentException("Clue cannot be null.");
  if (this.associatedClue != null) throw new IllegalStateException(name + " already has a clue.");
  this.associatedClue = c;
}

public String getName() {
  return name;
}

public String getSpecies() {
  return species;
}

public String getHabitat() {
  return habitat;
}

public String getDiet() {
  return diet;
}

public String getStatus() {
  return status;
}

public String getBehaviour() {
  return behaviour;
    }
}
