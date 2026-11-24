<div align="center">
  <h1> 
    <img src="screenshots/zoo-quest-logo.png" width="300" />
  </h1>
  <p> 
  An Interactive Zoo Exploration and Investigation Game <br>
  Made By Group 6 - <strong>cup of code</strong> (CS 2101) <br>
  </p>
  <p> 
  A project rquired for the course: <em><strong>OOP 101: Object Oriented Programming</strong></em> 
  </p>
</div>

##

## 📙 Description

**Zoo Quest** is a Java-based application where the user can explore different zones in a zoo and interact with various animals. Each zone has a mystery game, and the system will give the user a clue that can help solve the mystery. After getting enough clues and solving the mystery (find the animals), the user can unlock and move to the next zone until they finish the whole game.

## 🔎 Problems being addressed

Many children cannot visit physical zoos due to geographic distance, high travel costs, or lack of local facilities. This can also affect the teachers because they cannot organize field trips and need accessible digital tools to teach biodiversity. But children have easy access to the internet and gadgets to communicate with one another. At the same time, they only spend hours on gadgets consuming passive content (like short videos) and online games, which disconnects them from learning about nature and wildlife. This also affects their parents because they struggle to manage excessive, unhealthy screen time at home. 

This system will bridge the educational divide by granting every child, regardless of family income or location a free access to wildlife learning, while redirecting mindless screen time toward interactive mystery-solving that builds early environmental empathy and critical thinking skills.

## 🎯 Target Users

1. **Children** - They can use this system to explore virtual zoos and their biomes, complete the task (find the animal), and learn their habitat, diet, and behavior. <br>
2. **Teachers/Parents** - Use the System as an interactive learning activity to introduce children to animals and wildlife. 

## 📋 Main Features

1. **User Interface** <br> 
A user interface will feature a child-friendly, colorful theme, large button, simple icons, and clear navigation. It includes a menu page, biome map, achievements, and animal information panel. <br>
2. **Biome and Animal Interaction** <br> 
Virtual biomes such as Jungle, Safari, and Aquatic that children can freely explore, and each biome contains its own corresponding animals that children can view their habitat, diet, sounds, and behavior. <br>
3. **Mystery Game** <br> 
A clue-based game in which children (users) observe animals with or without given clues and submit guesses to solve each biome's wildlife mystery case. <br>
4. **Achievments** <br> 
Badges or titles such as “First Clue Collected” or “Master Explorer” are awarded for exploring biomes, learning animal facts, and solving cases. <br>

## ⌨️ Technologies Used:

1. **Java** - The main programming language of the system
2. **Java Swing** - Used for the GUI implementation
3. **Java AWT** - Used for screen navigation.
4. **Java Stanadard I/O** - Used for loading local animal and badge visual assets.

## 🧩 OOP Principles Demonstrated

- **Encapsulation** <br> Protects game state and data integrity by restricting direct access to internal fields. Core attributes (`isLocked` in `Biome`, `isRevealed` in `MysteryClue`, `isSolved` in `MysteryGame`, and `userProgress` in `UserProfile`) are kept private and modified strictly through controlled methods (`unlockBiome()`, `reveal()`, `solveMystery()`, `addAchievements()`). <br>
- **Abstraction** <br> Hiding background mechanics and exposing only clear interfaces to the player and GUI. `GameManager` and `MysteryGame` handle state transitions, guess verification (`verifyGuess()`), and biome progression behind the scenes, while GUI panels (`BiomeViewPanel`, `MysteryGamePanel`) simply trigger high-level actions.<br>
- **Inheritance** <br> Code reuse by building specific animal and zone types from common parent templates. GUI screens (`MainMenuPanel`, `BiomeViewPanel`, `AnimalViewPanel`, `MysteryGamePanel`, `AchievementViewPanel`) inherit standard layout and rendering capabilities from Java Swing's JPanel, while custom exceptions (`ZoneLockedException`, `InsufficientCluesException`, `AnimalNotFoundException`) extend Java's base `Exception` class.<br>
- **Plymorphism** <br> Letting different objects respond to the same action in their own distinct way. Different Animal instances execute `displayBehaviour()` and `getClue()` dynamically based on their specific species, habitat, and diet data. Action listeners across distinct UI components also respond uniquely to user click events.<br>

## 📝 Instructions for running the program

1. Download the zip file and extract it.
2. Inside the ZooQuest folder, right click the src folder and select **Open in Terminal**.
3. Compile the program using the command:
```
javac main/*.java
```
4. Run the main file using the command:
```
java main.ZooQuest
```

## 📷 Sample Screenshots

## 🔒 Known Limitations

1. The system will only include a limited number of zoo biomes and animals.
2. The game will only have pre-made clues and questions.
3. The system will focus on basic 2D GUI interaction and will not include 3D graphics.
4. The animal information and clues will be based on pre-set data stored in the system.

## 🗝️ Future Improvements

1. Expand the encyclopedia with additional biomes (e.g., Arctic, Desert) and a broader roster of wildlife species.
2. Introduce randomized clue generation and dynamic question banks to increase replayability.
3. Integrate richer multimedia assets, such as authentic animal audio recordings and animated 2D sprites.
4. Implement a stronger save feature that stores player progress in a more secured file format.
5. Package the application as a standalone executable version for easier installation and distribution.

## 👥 Project Contributors

<table>
<tr>
    <th> Name </th>
    <th> Contributions </h>
</tr>
<tr>
    <td> Arias, Jayro J. </td>
    <td>
        <ul>
            <li> Develops the program flow and user interaction flow of the ZooQuest application. </li>
            <li> Maintains the README, testing documentation, screenshots, and assists in checking the application's functionality. </li>
        </ul>
    </td>
</tr>
<tr>
    <td> Carenan, Ahron Simeon D.  </td>
    <td>
        <ul>
            <li> Coordinates the implementation of Java classes, methods, and OOP features.  </li>
            <li> Develop the UI/UX design of the program. </li>
        </ul>
    </td>
</tr>
<tr>
    <td> De Luna, Janelle H. </td>
    <td>
        <ul>
            <li> Develops system requirements, class designs, UML diagrams, and overall application structure. </li>
            <li> Helps plan the GUI navigation and gameplay sequence, including biome exploration and mystery game progression. </li>
        </ul>
    </td>
</tr>
</table>


 
