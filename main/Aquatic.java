package main;

/** NEW CODE: concrete animal type created by Animal.create('A', ...). */
public class Aquatic extends Animal {

    public Aquatic(String name, String species, String habitat, String diet,
            String status, String behaviour, String uniqueFeature) {
        super(name, species, habitat, diet, status, behaviour, uniqueFeature);
    }

    @Override
    public String getAnimalType() { return "Aquatic"; }

    @Override
    public String getMovementStyle() { return "Swims and glides through open water"; }

    @Override
    public String getSpriteTemplate() { return "sprites/aquatic_template.png"; }
}
