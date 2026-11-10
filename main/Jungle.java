package main;

import java.util.List; // NEW CODE

/** NEW CODE: concrete animal type created by Animal.create('J', ...). */
public class Jungle extends Animal {

    // NEW CODE: the ten jungle animals. Format per animal:
    // { name, species, diet, status, behaviour, uniqueFeature (description), clueHint }
    private static final String[][] ANIMAL_DATA = {
        {"Tiger", "Panthera tigris", "Carnivore", "Endangered",
         "Ambushes prey alone and swims across rivers with ease",
         "Orange coat with black stripes that are unique to every tiger",
         "I am the biggest cat in the world and my stripes are like fingerprints."},

        {"Gorilla", "Gorilla gorilla", "Herbivore", "Critically Endangered",
         "Lives in a family troop led by a silverback",
         "Silver-grey back fur on adult males and strong arms for knuckle-walking",
         "I am the largest living primate and troop follows a silverback leader."},

        {"Monkey", "Cebus capucinus", "Omnivore", "Varies by Species",
         "Travles in noisy troops and grooms friends to bond",
         "Long grasping tail that works like a fifth hand",
         "I swing through the treetops using my tail and never travel alone"},

        {"Eagle", "Harpia harpyja", "Carnivore", "Near Threatened",
         "Hunts from a perch and snatches monkeys and sloths from the trees",
         "Huge talons as big as a bear's claws and a crown of feathers",
         "I rule the rainforest sky and my talons are as big as a bear's claws."},

        {"Lemur", "Lemur catta", "Herbivore", "Endangered",
         "Sunbathes in groups and scent-marks with its tail",
         "Black-and-white ringed tail held high like a flag",
         "I come from Madagascar and wave my striped tail like a flag"},

        {"Koala", "Phascolarctos cinereus", "Herbivore", "Vulnerable",
         "Sleeps 18-22 hours a day and spends most of its life alone",
         "Fluffy round ears and fingerprints almost like a human's",
         "I'm not a bear! I munch eucalyptus leaves and sleep most of the day."},

        {"Deer", "Cervus elaphus", "Herbivore", "Varies by species",
         "Most active at dawn and dusk; males clash antlers in rutting season",
         "Antlers that fall off and regrow every year",
         "I grow a new set of antlers every year and come out at sunrise and sunset."},

        {"Parrot", "Ara macao", "Herbivore", "Many species threatened",
         "Flocks together and copies the sounds it hears",
         "Bright feathers and a strong curved beak for cracking nuts",
         "I wear all the colors of the rainbow and can copy your voice."},

        {"Capybara", "Hydrochoerus hydrochoerus", "Herbivore", "Least Concern",
         "Semi-aquatic and lives in groups of up to about 20",
         "Webbed feet and eyes and nose set high on its head for swimming",
         "I am the world's largest rodent and I love a good swim."},

        {"Komodo dragon", "Varanus komodoensis", "Carnivore", "Endangered",
         "Ambushes prey alone, then tracks it after a venomous bite",
         "Forked tongue that smells the air and a venomous bite",
         "I am the largest lizard alive and I live on islands in Indonesia."},
    };

    // NEW CODE: builds the animals of this type (each with its clue) for the biome they live in
    static List<Animal> createAnimals(String biomeName) {
        return Animal.fromRows('J', biomeName, ANIMAL_DATA);
    }

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

    @Override
    public String getRevealMessage() { return "It swings down from the canopy and drops in front of you!"; } // NEW CODE
}