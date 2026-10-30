package main;

/** NEW CODE: concrete animal type created by Animal.create('S', ...). */
public class Safari extends Animal {

    public Safari(String name, String species, String habitat, String diet,
            String status, String behaviour, String uniqueFeature) {
        super(name, species, habitat, diet, status, behaviour, uniqueFeature);
    }

    @Override
    public String getAnimalType() { return "Safari"; }

    @Override
    public String getMovementStyle() { return "Gallops and roams across open grassland"; }

    @Override
    public String getSpriteTemplate() { return "sprites/safari_template.png"; }
}
