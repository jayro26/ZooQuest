package main.exceptions;

// Thrown when an animal cannot be found in the active biome.
public class AnimalNotFoundException extends Exception {
    private final String searchedName;
    private final String biomeName;

    public AnimalNotFoundException(String searchedName, String biomeName) {
        super("Animal '" + searchedName + "' does not exist in " + biomeName);
        this.searchedName = searchedName;
        this.biomeName = biomeName;
    }

    public String getSearchedName() { return searchedName; }
    public String getBiomeName() { return biomeName; }
}
