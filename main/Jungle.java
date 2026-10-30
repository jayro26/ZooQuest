package main;

/** NEW CODE: concrete animal type created by Animal.create('J', ...). */
public class Jungle extends Animal {

    public Jungle(String name, String species, String habitat, String diet,
            String status, String behaviour, String uniqueFeature) {
        super(name, species, habitat, diet, status, behaviour, uniqueFeature);
    }

    @Override
    public String getAnimalType() { return "Jungle"; }

    @Override
    public String getMovementStyle() { return "Swings, climbs and prowls through the canopy"; }

    @Override
    public String getSpriteTemplate() { return "sprites/jungle_template.png"; }
}
