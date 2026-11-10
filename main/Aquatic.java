package main;

import java.util.List; // NEW CODE

/** NEW CODE: concrete animal type created by Animal.create('A', ...). */
public class Aquatic extends Animal {

    // NEW CODE: the ten aquatic animals. Format per animal:
    // { name, species, diet, status, behaviour, uniqueFeature (description), clueHint }
    private static final String[][] ANIMAL_DATA = {
        {"Tuna", "Thunnus thynnus", "Carnivore", "Varies (southern bluefin: Endangered)",
         "Swims in fast schools and migrates across whole oceans",
         "Torpedo-shaped body built for speed",
         "I'm built like a torpedo and I cross entire oceans."},

        {"Shark", "Carcharodon carcharias", "Carnivore", "Vulnerable",
         "Ambushes seals from below and sometimes leaps out of the water",
         "Rows of teeth that are replaced all through its life",
         "I lose thousands of teeth in my life, but I always grow more."},

        {"Whale", "Balaenoptera musculus", "Carnivore", "Endangered",
         "Sings long, deep calls to communicate across the ocean",
         "Largest animal ever with a heart the size of a small car",
         "I am the largest animal on Earth and I sing songs across the sea."},

        {"Penguin", "Aptenodytes forsteri", "Carnivore", "Near threatened",
         "Males huddle together to keep the eggs warm",
         "Flipper-like wings for swimming and thick, waterproof feathers",
         "I can't fly, but I zoom underwater and dad keeps our egg warm."}, 

        {"Swordfish", "Xiphias gladius", "Carnivore", "Least Concern",
         "Slashes at prey with its bill and usually swims alone",
         "Long, flat bill shaped like a sword",
         "I swim alone and my long, flat bill is like a sword."},

        {"Sea lion", "Zalophus californianus", "Carnivore", "Least Concern",
         "Lives in noisy colonies and walks on its flippers",
         "Visible ear flaps and front flippers that can turn to walk",
         "I bark loudly and have little ear flaps, unlike seals."},

        {"Dolphin", "Tursiops truncatus", "Carnivore", "Least concern",
         "Hunts in pods and finds food using echolocation",
         "Sleeps with half its brain awake and uses whistles as names",
         "I click and whistle to find food and call my friends."},

        {"Octupus", "Octupus vulgaris", "Carnivore", "Least Concern",
         "Hides in crevices and changes color to disappear",
         "Three hearts, blue blood and eight clever arms",
         "I have eight arms, three hearts and I can change my color."},

        {"Stringray", "Dasyatis americana", "Carnivore", "Varies by species",
         "Hides under the sand and defends itself with a barbed tail",
         "Flat, wing-like body with eyes on top of its head",
         "I look like a flying carpet and I hide under the sand."},

        {"Manta ray", "Mobula birostris", "Carnivore", "Endagered",
         "Visits cleaning stations where small fish clean its skin",
         "Wingspan up to about 7 meters and a big brain for a fish",
         "My wings can stretch wider than a boat and I seat tiny plankton"},
    };

    // NEW CODE: builds the animals of this type (each with its clue) for the biome they live in
    static List<Animal> createAnimals(String biomeName) {
        return Animal.fromRows('A', biomeName, ANIMAL_DATA);
    }

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

    @Override
    public String getRevealMessage() { return "It leaps out of the water with a big splash!"; } // NEW CODE
}