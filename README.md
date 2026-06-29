# Retro Snake Game

A vibrant, modern recreation of the classic arcade Snake game built in Java using the Swing and AWT frameworks. The game features smooth visuals, dynamic difficulty settings, a responsive UI, and custom-synthesized retro audio effects.

## Features

- **Retro Aesthetics**: Neon-style color palettes, custom gradients, and a sleek dark theme.
- **Difficulty Selection Menu**: Select between **Easy**, **Medium**, and **Hard** speeds.
- **Quit Option**: Exit the game directly from the difficulty selection screen.
- **Programmatic Audio**: Classic 8-bit sound effects when the snake eats food, generated dynamically using Java's built-in `javax.sound.sampled` API (no external files required).
- **HUD & Score Tracking**: Real-time score display and persistent local High Score tracking.
- **Responsive Controls**: Fully customizable controls for gameplay, pause, resume, and restart.

## Controls

### Main Menu / Difficulty Selection
- **Up Arrow / W**: Navigate Up
- **Down Arrow / S**: Navigate Down
- **ENTER / SPACE**: Select Option

### During Gameplay
- **Arrow Keys / WASD**: Change Snake Direction
- **P / ESCAPE**: Pause / Resume the game

### Game Over Screen
- **SPACE / R**: Restart the game
- **M**: Return to the Main Menu

## Requirements

- **Java Development Kit (JDK)**: JDK 8 or higher is required to build and run.

## How to Run

1. Compile the Java files:
   ```bash
   javac SnakeGame.java
   ```
2. Run the application:
   ```bash
   java SnakeGame
   ```
