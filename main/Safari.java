package main;

import java.util.List; // NEW CODE

/** NEW CODE: concrete animal type created by Animal.create('S', ...). */
public class Safari extends Animal {

    // NEW CODE: the ten safari animals. Format per animal:
    // { name, species, diet, status, behaviour, uniqueFeature (description), clueHint }
    private static final String[][] ANIMAL_DATA = {
        {"Zebra", "Equus quagga", "Herbivore", "Near Threatened",
         "Travels in herds, where moving stripes confuse predators",
         "Black-and-white stripes that are unique to each zebra",
         "No two of my stripe patterns are the same."},

        {"Rhino", "Diceros bicornis", "Herbivore", "Critically Endangered",
         "Lives alone with poor eyesight but sharp hearing and smell",
         "Two horns made of keratin, the same stuff as your fingernails",
         "My two horns are made of the same stuff as your fingernails."},

        {"Hippopotamus", "Hippopotamus amphibius", "Herbivore", "Vulnerable",
         "Rests in water by day and grazes on land at night",
         "Enormous jaws and skin that oozes a red, sunscreen-like liquid",
         "I soak in the river all day and come out at night to eat grass."},

        {"Giraffe", "Giraffa camelopardalis", "Herbivore", "Endangered",
         "Lives in loose herds and sleeps only in short naps",
         "Tallest animal on land with a long dark tongue for grabbing leaves",
         "I am the tallest animal on Earth and I eat the highest acacia leaves."},

        {"Elephant", "Loxodonta africana", "Herbivore", "Vulnerable",
         "Female-led herds with a famous memory",
         "Trunk with about 40,000 muscles and huge fan-shaped ears",
         "My trunk is my nose, my hand and my drinking straw all in one."},

        {"Hyena", "Crocuta crocuta", "Omnivore", "Least Concern",
         "Lives in female-led clans and makes laughing calls",
         "Powerful jaws that can crush bones",
         "I may sound like I'm laughing, but I'm the boss of my clan."},

        {"Ostrich", "Struthio camelus", "Omnivore", "Least Concern",
         "Cannot fly but runs up to about 70 km/h",
         "Largest bird on Earth with enormous eyes",
         "I can't fly, but I can outrun almost anything on the savanna."},

        {"Buffalo", "Syncerus caffer", "Herbivore", "Near Threatened",
         "Herds stand together to protect their calves from predators",
         "Heavy curved horns that meet across the forehead",
         "My herd always has my back, and my curved horns are heavy."},

        {"Nile Crocodile", "Crocodylus niloticus", "Carnivore", "Least Concern",
         "Basks in the sun and ambushes prey at the water's edge",
         "Eyes and nostrils on top of its head so it can hide underwater",
         "I float like a log in the river, then SNAP!"},

        {"Vulture", "Gyps africanus", "Carnivore", "Critically Endangered",
         "Soars on warm air currents and cleans up carcasses",
         "Bald head that stays clean while feeding and very strong stomach acid",
         "I'm nature's clean-up crew and I ride warm air high above the savanna."},
    };

    // NEW CODE: builds the animals of this type (each with its clue) for the biome they live in
    static List<Animal> createAnimals(String biomeName) {
        return Animal.fromRows('S', biomeName, ANIMAL_DATA);
    }

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

    @Override
    public String getRevealMessage() { return "It gallops out of the tall grass and skids to a stop!"; } // NEW CODE
}