<div align="center">
  <h1> 
    <img src="screenshots/zoo-quest-logo.png" width="300">
  </h1>
  <p> 
  An Interactive Zoo Exploration and Investigation Game <br>
  Made By Group 6 - <strong>cup of code</strong> (CS 2101) <br>
  </p>
  <p> 
  A project rquired for the course: <em><strong>OOP 101: Object Oriented Programming</strong></em> 
  </p>
</div>

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
Virtual biomes such as Jungle, Safari, and Aquatic that children can freely explore, and each biome contains its own corresponding animals that children can view their habitat, diet, and behavior. <br>
3. **Mystery Game** <br> 
A clue-based game in which children (users) observe animals with or without given clues and submit guesses to solve each biome's wildlife mystery case. <br>
4. **Achievments** <br> 
Badges or titles such as “Jungle Explorer” or “Safari Scout” are awarded for exploring biomes, learning animal facts, and solving cases. <br>

## ⌨️ Technologies Used:

1. **Java** - The main programming language of the system
2. **Java Swing** - Used to build the graphical user interface (GUI).
3. **Java 2D (AWT)** - Used to render custom interface elements such as buttons, borders, shadows and animations.
4. **Image I/O** - A Java API used for loading local image assets (eg. animal illustrations and biome backgrounds).
5. **Text-Based Save Files** - Used for file handling to store each palyers progress.
6. **javac** - To compile the Java source code into executable bytecode.
7. **Visual Studio Code** - The IDE used to write, run and debug the source code.
8. **Git and Github** - For version control and sharing the project repository.

## 🧩 OOP Principles Demonstrated

- **Encapsulation** <br> Protects game state and data integrity by restricting direct access to internal fields. Core attributes (`isLocked` in `Biome`, `isRevealed` in `MysteryClue`, `isSolved` in `MysteryGame`, and `userProgress` in `UserProfile`) are kept private and modified strictly through controlled methods (`unlockBiome()`, `reveal()`, `solveMystery()`, `addAchievements()`). <br>
- **Abstraction** <br> Hiding background mechanics and exposing only clear interfaces to the player and GUI. `GameManager` and `MysteryGame` handle state transitions, guess verification (`verifyGuess()`), and biome progression behind the scenes, while GUI panels (`BiomeViewPanel`, `MysteryGamePanel`) simply trigger high-level actions.<br>
- **Inheritance** <br> Code reuse by building specific animal and zone types from common parent templates. GUI screens (`MainMenuPanel`, `BiomeViewPanel`, `AnimalViewPanel`, `MysteryGamePanel`, `AchievementViewPanel`) inherit standard layout and rendering capabilities from Java Swing's JPanel, while custom exceptions (`ZoneLockedException`, `InsufficientCluesException`, `AnimalNotFoundException`) extend Java's base `Exception` class.<br>
- **Plymorphism** <br> Letting different objects respond to the same action in their own distinct way. Different Animal instances execute `displayBehaviour()` and `getClue()` dynamically based on their specific species, habitat, and diet data. Action listeners across distinct UI components also respond uniquely to user click events.<br>

## 📝 Instructions for running the program

1. Download the zip file found in the Releases and extract it.
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

<div>
    <img src="screenshots/Screenshot 2026-11-26 205817.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 210104.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 210114.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 210913.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 211647.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 211120.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 211202.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 210849.png" width="250">
    <img src="screenshots/Screenshot 2026-11-26 210856.png" width="250">
</div>

## 🔒 Known Limitations

1. The system will only include a limited number of zoo biomes and animals.
2. The game will only have pre-made clues and questions.
3. The system will focus on basic 2D GUI interaction and will not include 3D graphics.
4. The animal information and clues will be based on pre-set data stored in the system.

## 🗝️ Future Improvements

1. Expand the encyclopedia with additional biomes (e.g., Arctic, Desert) and a broader roster of wildlife species.
2. Introduce randomized clue generation and dynamic question banks to increase replayability.
3. Integrate multimedia assets, such as background music, animal audio recordings and animated 2D sprites.
4. Implement a stronger save feature that stores player progress in a more secured file format.
5. Package the application as a standalone executable version for easier installation and distribution.

## 👥 Project Contributors

<table>
  <tr align="center">
      <th style="background-color: #E7EAED;"> </th>
      <th style="background-color: #FF9F0A;"> Arias, Jayro J. </th>
      <th style="background-color: #B562FF;"> Carenan, Ahron Simeon D. </th>
      <th style="background-color: #f76868;"> De Lumen, Janelle H. </th>
  </tr>
  <tr>
    <td style="background-color: #E7EAED;"> Roles </td>
    <td>
        <ul>
            <li> Develop the UI/UX design of the program. </li>
            <li> Maintains the README, testing documentation, screenshots, and assists in checking the application's functionality. </li>
        </ul>
    </td>
    <td>
        <ul>
            <li> Coordinates the implementation of Java classes, methods, and OOP features.  </li>
            <li> Develops the program flow and user interaction flow of the ZooQuest application. </li>
        </ul>
    </td>
    <td>
        <ul>
            <li> Develops system requirements, class designs, UML diagrams, and overall application structure. </li>
            <li> Helps plan the GUI navigation and gameplay sequence, including biome exploration and mystery game progression. </li>
        </ul>
    </td>
  </tr>
</table>
