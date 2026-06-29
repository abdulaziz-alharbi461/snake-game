import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.*;

public class SnakeGame extends JFrame {
    public SnakeGame() {
        setTitle("Retro Snake Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(400, 400));
        
        // Set App Icon
        try {
            ImageIcon icon = new ImageIcon("snake_icon.jpg");
            if (icon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                setIconImage(icon.getImage());
            } else {
                setIconImage(createFallbackIcon());
            }
        } catch (Exception e) {
            setIconImage(createFallbackIcon());
        }

        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private Image createFallbackIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        g2.setColor(new Color(18, 18, 18));
        g2.fillRoundRect(0, 0, 64, 64, 16, 16);
        
        // Draw simple snake head and apple
        g2.setColor(new Color(76, 175, 80));
        g2.fillRoundRect(8, 20, 24, 24, 8, 8);
        
        g2.setColor(new Color(239, 83, 80));
        g2.fillOval(38, 24, 16, 16);
        
        // Apple leaf
        g2.setColor(new Color(76, 175, 80));
        g2.fillOval(44, 20, 6, 8);
        
        // Snake eyes
        g2.setColor(Color.BLACK);
        g2.fillOval(20, 26, 4, 4);
        g2.fillOval(20, 34, 4, 4);
        
        g2.dispose();
        return image;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SnakeGame());
    }

    private static class GamePanel extends JPanel implements ActionListener {
        private static final int GRID_COLS = 24;
        private static final int GRID_ROWS = 24;
        private static final int DEFAULT_UNIT_SIZE = 25; // Base dimensions: 600x600

        private enum GameState {
            MENU, PLAYING, PAUSED, GAME_OVER
        }

        private enum Difficulty {
            EASY(150, "Easy"),
            MEDIUM(95, "Medium"),
            HARD(55, "Hard"),
            QUIT(0, "Quit");

            final int delay;
            final String label;

            Difficulty(int delay, String label) {
                this.delay = delay;
                this.label = label;
            }
        }

        private GameState gameState = GameState.MENU;
        private Difficulty selectedDifficulty = Difficulty.MEDIUM;

        // Snake parts in grid coordinates
        private final List<Point> snake = new ArrayList<>();
        private int snakeLength = 3;
        
        // Food grid coordinates
        private Point food;
        
        // Direction
        private char direction = 'R'; // U, D, L, R
        private char nextDirection = 'R'; 
        
        private int score = 0;
        private int highScore = 0;
        
        private Timer timer;
        private final Random random = new Random();

        public GamePanel() {
            setPreferredSize(new Dimension(GRID_COLS * DEFAULT_UNIT_SIZE, GRID_ROWS * DEFAULT_UNIT_SIZE));
            setBackground(new Color(18, 18, 18)); // Sleek dark theme background
            setFocusable(true);
            
            // Add Key Listener
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    
                    if (gameState == GameState.MENU) {
                        switch (keyCode) {
                            case KeyEvent.VK_UP:
                            case KeyEvent.VK_W: {
                                int currentIdx = selectedDifficulty.ordinal();
                                int nextIdx = (currentIdx - 1 + Difficulty.values().length) % Difficulty.values().length;
                                selectedDifficulty = Difficulty.values()[nextIdx];
                                repaint();
                                break;
                            }
                            case KeyEvent.VK_DOWN:
                            case KeyEvent.VK_S: {
                                int currentIdx = selectedDifficulty.ordinal();
                                int nextIdx = (currentIdx + 1) % Difficulty.values().length;
                                selectedDifficulty = Difficulty.values()[nextIdx];
                                repaint();
                                break;
                            }
                            case KeyEvent.VK_ENTER:
                            case KeyEvent.VK_SPACE: {
                                if (selectedDifficulty == Difficulty.QUIT) {
                                    System.exit(0);
                                } else {
                                    timer.setDelay(selectedDifficulty.delay);
                                    resetGame();
                                    gameState = GameState.PLAYING;
                                    repaint();
                                }
                                break;
                            }
                        }
                        return;
                    }
                    
                    if (gameState == GameState.GAME_OVER) {
                        if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_R) {
                            timer.setDelay(selectedDifficulty.delay);
                            resetGame();
                            gameState = GameState.PLAYING;
                        } else if (keyCode == KeyEvent.VK_M) {
                            gameState = GameState.MENU;
                            repaint();
                        }
                        return;
                    }
                    
                    if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE) {
                        if (gameState == GameState.PLAYING) {
                            gameState = GameState.PAUSED;
                        } else if (gameState == GameState.PAUSED) {
                            gameState = GameState.PLAYING;
                        }
                        repaint();
                        return;
                    }
                    
                    if (gameState == GameState.PAUSED) return;

                    switch (keyCode) {
                        case KeyEvent.VK_LEFT:
                        case KeyEvent.VK_A:
                            if (direction != 'R') {
                                nextDirection = 'L';
                            }
                            break;
                        case KeyEvent.VK_RIGHT:
                        case KeyEvent.VK_D:
                            if (direction != 'L') {
                                nextDirection = 'R';
                            }
                            break;
                        case KeyEvent.VK_UP:
                        case KeyEvent.VK_W:
                            if (direction != 'D') {
                                nextDirection = 'U';
                            }
                            break;
                        case KeyEvent.VK_DOWN:
                        case KeyEvent.VK_S:
                            if (direction != 'U') {
                                nextDirection = 'D';
                            }
                            break;
                    }
                }
            });
            
            startGame();
        }

        private void startGame() {
            // Spawn default empty board setup
            snake.add(new Point(GRID_COLS / 2, GRID_ROWS / 2));
            food = new Point(0, 0);
            timer = new Timer(selectedDifficulty.delay, this);
            timer.start();
        }
        
        private void resetGame() {
            snake.clear();
            snakeLength = 3;
            score = 0;
            direction = 'R';
            nextDirection = 'R';
            
            // Initial snake position in the middle
            int startX = GRID_COLS / 2;
            int startY = GRID_ROWS / 2;
            for (int i = 0; i < snakeLength; i++) {
                snake.add(new Point(startX - i, startY));
            }
            
            spawnFood();
            if (timer != null && !timer.isRunning()) {
                timer.start();
            }
        }
        
        private void spawnFood() {
            while (true) {
                int fx = random.nextInt(GRID_COLS);
                int fy = random.nextInt(GRID_ROWS);
                Point p = new Point(fx, fy);
                
                boolean onSnake = false;
                for (Point part : snake) {
                    if (part.equals(p)) {
                        onSnake = true;
                        break;
                    }
                }
                if (!onSnake) {
                    food = p;
                    break;
                }
            }
        }
        
        private void move() {
            direction = nextDirection;
            Point head = snake.get(0);
            int nextX = head.x;
            int nextY = head.y;
            
            switch (direction) {
                case 'U':
                    nextY--;
                    break;
                case 'D':
                    nextY++;
                    break;
                case 'L':
                    nextX--;
                    break;
                case 'R':
                    nextX++;
                    break;
            }
            
            Point newHead = new Point(nextX, nextY);
            snake.add(0, newHead);
            
            if (newHead.equals(food)) {
                score += 10;
                if (score > highScore) {
                    highScore = score;
                }
                playEatSound();
                spawnFood();
            } else {
                snake.remove(snake.size() - 1);
            }
        }
        
        private void playEatSound() {
            new Thread(() -> {
                try {
                    float sampleRate = 16000f;
                    AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, true);
                    SourceDataLine line = AudioSystem.getSourceDataLine(format);
                    line.open(format);
                    line.start();
                    
                    int durationMs = 100;
                    int numSamples = (int) (sampleRate * durationMs / 1000.0);
                    byte[] buffer = new byte[numSamples];
                    
                    double f0 = 500.0;
                    double f1 = 1000.0;
                    double T = durationMs / 1000.0;
                    
                    for (int i = 0; i < numSamples; i++) {
                        double t = (double) i / sampleRate;
                        double phase = 2.0 * Math.PI * (f0 * t + 0.5 * (f1 - f0) * t * t / T);
                        double envelope = 1.0 - (t / T);
                        buffer[i] = (byte) (Math.sin(phase) * 100.0 * envelope);
                    }
                    
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // Fail silently if audio is unavailable
                }
            }).start();
        }
        
        private void checkCollisions() {
            Point head = snake.get(0);
            
            if (head.x < 0 || head.x >= GRID_COLS || head.y < 0 || head.y >= GRID_ROWS) {
                gameState = GameState.GAME_OVER;
            }
            
            for (int i = 1; i < snake.size(); i++) {
                if (head.equals(snake.get(i))) {
                    gameState = GameState.GAME_OVER;
                    break;
                }
            }
            
            if (gameState == GameState.GAME_OVER) {
                timer.stop();
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameState == GameState.PLAYING) {
                move();
                checkCollisions();
            }
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            
            // Calculate dynamic unit size to preserve 1:1 ratio
            int unitSize = Math.min(panelWidth / GRID_COLS, panelHeight / GRID_ROWS);
            unitSize = Math.max(unitSize, 5);
            
            int boardWidth = GRID_COLS * unitSize;
            int boardHeight = GRID_ROWS * unitSize;
            
            int offsetX = (panelWidth - boardWidth) / 2;
            int offsetY = (panelHeight - boardHeight) / 2;
            
            if (gameState == GameState.MENU) {
                drawMenuScreen(g2d, unitSize, offsetX, offsetY, boardWidth, boardHeight);
                return;
            }
            
            // Fill background
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, panelWidth, panelHeight);
            
            g2d.setColor(new Color(18, 18, 18));
            g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);
            
            // Draw grid lines
            g2d.setColor(new Color(30, 30, 30));
            for (int i = 0; i <= GRID_COLS; i++) {
                g2d.drawLine(offsetX + i * unitSize, offsetY, offsetX + i * unitSize, offsetY + boardHeight);
            }
            for (int j = 0; j <= GRID_ROWS; j++) {
                g2d.drawLine(offsetX, offsetY + j * unitSize, offsetX + boardWidth, offsetY + j * unitSize);
            }
            
            // Draw Food
            g2d.setColor(new Color(239, 83, 80)); // Neon red
            g2d.fillRoundRect(offsetX + food.x * unitSize + 2, offsetY + food.y * unitSize + 2, unitSize - 4, unitSize - 4, 8, 8);
            g2d.setColor(new Color(255, 138, 128));
            g2d.fillOval(offsetX + food.x * unitSize + unitSize / 4 + 1, offsetY + food.y * unitSize + unitSize / 4 + 1, unitSize / 4, unitSize / 4);
            
            // Draw Snake
            for (int i = 0; i < snake.size(); i++) {
                Point segment = snake.get(i);
                int sx = offsetX + segment.x * unitSize;
                int sy = offsetY + segment.y * unitSize;
                
                if (i == 0) {
                    // Snake Head
                    g2d.setColor(new Color(76, 175, 80)); // Neon green
                    g2d.fillRoundRect(sx + 1, sy + 1, unitSize - 2, unitSize - 2, 10, 10);
                    
                    // Eyes
                    g2d.setColor(Color.BLACK);
                    int eyeSize = Math.max(unitSize / 6, 2);
                    int offset = Math.max(unitSize / 5, 2);
                    if (direction == 'R' || direction == 'L') {
                        g2d.fillOval(sx + (direction == 'R' ? unitSize - offset - eyeSize : offset), sy + offset, eyeSize, eyeSize);
                        g2d.fillOval(sx + (direction == 'R' ? unitSize - offset - eyeSize : offset), sy + unitSize - offset - eyeSize, eyeSize, eyeSize);
                    } else {
                        g2d.fillOval(sx + offset, sy + (direction == 'D' ? unitSize - offset - eyeSize : offset), eyeSize, eyeSize);
                        g2d.fillOval(sx + unitSize - offset - eyeSize, sy + (direction == 'D' ? unitSize - offset - eyeSize : offset), eyeSize, eyeSize);
                    }
                } else {
                    // Body
                    float ratio = (float) i / snake.size();
                    int r = (int) (76 + ratio * 40);
                    int gr = (int) (175 - ratio * 50);
                    int b = (int) (80 + ratio * 20);
                    g2d.setColor(new Color(r, gr, b));
                    g2d.fillRoundRect(sx + 2, sy + 2, unitSize - 4, unitSize - 4, 6, 6);
                }
            }
            
            drawHUD(g2d, unitSize, offsetX, offsetY, boardWidth);
            
            if (gameState == GameState.PAUSED) {
                drawPauseScreen(g2d, unitSize, offsetX, offsetY, boardWidth, boardHeight);
            } else if (gameState == GameState.GAME_OVER) {
                drawGameOverScreen(g2d, unitSize, offsetX, offsetY, boardWidth, boardHeight);
            }
        }
        
        private void drawHUD(Graphics2D g2d, int unitSize, int offsetX, int offsetY, int boardWidth) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (unitSize * 0.7)));
            g2d.setColor(Color.WHITE);
            String scoreStr = "Score: " + score;
            String highscoreStr = "High Score: " + highScore;
            
            g2d.drawString(scoreStr, offsetX + (int) (unitSize * 0.8), offsetY + (int) (unitSize * 1.2));
            
            FontMetrics metrics = g2d.getFontMetrics();
            g2d.drawString(highscoreStr, offsetX + boardWidth - metrics.stringWidth(highscoreStr) - (int) (unitSize * 0.8), offsetY + (int) (unitSize * 1.2));
        }

        private void drawMenuScreen(Graphics2D g2d, int unitSize, int offsetX, int offsetY, int boardWidth, int boardHeight) {
            g2d.setColor(new Color(18, 18, 18));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            g2d.setColor(new Color(30, 30, 30));
            g2d.drawRect(offsetX, offsetY, boardWidth, boardHeight);
            
            g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (unitSize * 2.2)));
            g2d.setColor(new Color(76, 175, 80)); 
            String title = "S N A K E";
            FontMetrics fmTitle = g2d.getFontMetrics();
            g2d.drawString(title, offsetX + (boardWidth - fmTitle.stringWidth(title)) / 2, (int) (offsetY + boardHeight * 0.28));
            
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, (int) (unitSize * 0.8)));
            g2d.setColor(new Color(180, 180, 180));
            String subtitle = "Select Difficulty";
            FontMetrics fmSub = g2d.getFontMetrics();
            g2d.drawString(subtitle, offsetX + (boardWidth - fmSub.stringWidth(subtitle)) / 2, (int) (offsetY + boardHeight * 0.40));
            
            Difficulty[] difficulties = Difficulty.values();
            int startY = (int) (offsetY + boardHeight * 0.47);
            int spacing = (int) (unitSize * 1.8);
            
            g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (unitSize * 0.9)));
            FontMetrics fmMenu = g2d.getFontMetrics();
            
            for (int i = 0; i < difficulties.length; i++) {
                Difficulty diff = difficulties[i];
                String label = diff.label;
                int stringWidth = fmMenu.stringWidth(label);
                int x = offsetX + (boardWidth - stringWidth) / 2;
                int y = startY + i * spacing;
                
                if (diff == selectedDifficulty) {
                    int pWidth = (int) (stringWidth + unitSize * 2.0);
                    int pHeight = (int) (unitSize * 1.4);
                    int px = offsetX + (boardWidth - pWidth) / 2;
                    int py = y - (int) (unitSize * 1.0);
                    
                    Color pillColor;
                    switch (diff) {
                        case EASY:
                            pillColor = new Color(76, 175, 80, 40); 
                            g2d.setColor(new Color(76, 175, 80));
                            break;
                        case MEDIUM:
                            pillColor = new Color(255, 167, 38, 40); 
                            g2d.setColor(new Color(255, 167, 38));
                            break;
                        case HARD:
                            pillColor = new Color(239, 83, 80, 40); 
                            g2d.setColor(new Color(239, 83, 80));
                            break;
                        case QUIT:
                            pillColor = new Color(150, 150, 150, 40);
                            g2d.setColor(new Color(200, 200, 200));
                            break;
                        default:
                            pillColor = new Color(255, 255, 255, 40);
                            g2d.setColor(Color.WHITE);
                    }
                    g2d.setColor(pillColor);
                    g2d.fillRoundRect(px, py, pWidth, pHeight, 10, 10);
                    
                    // Text color
                    switch (diff) {
                        case EASY: g2d.setColor(new Color(76, 175, 80)); break;
                        case MEDIUM: g2d.setColor(new Color(255, 167, 38)); break;
                        case HARD: g2d.setColor(new Color(239, 83, 80)); break;
                        case QUIT: g2d.setColor(new Color(200, 200, 200)); break;
                        default: g2d.setColor(Color.WHITE); break;
                    }
                    g2d.drawString(label, x, y);
                } else {
                    g2d.setColor(new Color(120, 120, 120));
                    g2d.drawString(label, x, y);
                }
            }
            
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, (int) (unitSize * 0.6)));
            g2d.setColor(new Color(150, 150, 150));
            String footer1 = "Use Arrow Keys (↑/↓) or (W/S) to Navigate";
            String footer2 = "Press ENTER or SPACE to Select";
            FontMetrics fmFooter = g2d.getFontMetrics();
            g2d.drawString(footer1, offsetX + (boardWidth - fmFooter.stringWidth(footer1)) / 2, (int) (offsetY + boardHeight * 0.85));
            g2d.drawString(footer2, offsetX + (boardWidth - fmFooter.stringWidth(footer2)) / 2, (int) (offsetY + boardHeight * 0.91));
        }

        private void drawPauseScreen(Graphics2D g2d, int unitSize, int offsetX, int offsetY, int boardWidth, int boardHeight) {
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (unitSize * 1.6)));
            String pauseMsg = "PAUSED";
            FontMetrics metrics = g2d.getFontMetrics();
            g2d.drawString(pauseMsg, offsetX + (boardWidth - metrics.stringWidth(pauseMsg)) / 2, offsetY + boardHeight / 2 - (int)(unitSize * 0.8));
            
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, (int) (unitSize * 0.7)));
            String resumeMsg = "Press ESC or P to Resume";
            metrics = g2d.getFontMetrics();
            g2d.drawString(resumeMsg, offsetX + (boardWidth - metrics.stringWidth(resumeMsg)) / 2, offsetY + boardHeight / 2 + (int)(unitSize * 0.8));
        }

        private void drawGameOverScreen(Graphics2D g2d, int unitSize, int offsetX, int offsetY, int boardWidth, int boardHeight) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);
            
            g2d.setColor(new Color(239, 83, 80));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (unitSize * 1.8)));
            String gameOverMsg = "GAME OVER";
            FontMetrics metrics = g2d.getFontMetrics();
            g2d.drawString(gameOverMsg, (offsetX + (boardWidth - metrics.stringWidth(gameOverMsg)) / 2), (int)(offsetY + boardHeight / 2 - unitSize * 2.4));
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, (int) (unitSize * 0.8)));
            String finalScoreMsg = "Your Score: " + score;
            metrics = g2d.getFontMetrics();
            g2d.drawString(finalScoreMsg, offsetX + (boardWidth - metrics.stringWidth(finalScoreMsg)) / 2, offsetY + boardHeight / 2 - (int) (unitSize * 0.4));
            
            String highScoreMsg = "High Score: " + highScore;
            metrics = g2d.getFontMetrics();
            g2d.drawString(highScoreMsg, offsetX + (boardWidth - metrics.stringWidth(highScoreMsg)) / 2, offsetY + boardHeight / 2 + (int) (unitSize * 0.8));
            
            g2d.setColor(new Color(76, 175, 80));
            g2d.setFont(new Font("Segoe UI", Font.BOLD, (int) (unitSize * 0.72)));
            String restartMsg = "Press SPACE or R to Restart";
            String menuMsg = "Press M to Return to Main Menu";
            metrics = g2d.getFontMetrics();
            g2d.drawString(restartMsg, offsetX + (boardWidth - metrics.stringWidth(restartMsg)) / 2, offsetY + boardHeight / 2 + (int) (unitSize * 2.4));
            g2d.drawString(menuMsg, offsetX + (boardWidth - metrics.stringWidth(menuMsg)) / 2, offsetY + boardHeight / 2 + (int) (unitSize * 3.4));
        }
    }
}
