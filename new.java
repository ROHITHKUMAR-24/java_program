import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * QuantumSnake.java
 *
 * A Snake variant with novel mechanics:
 * - Portals linking two cells
 * - Moving, evasive food
 * - Modules (Magnet, Phase) picked up and activated via keys
 * - Rewind power-up that restores recent states
 *
 * Single-file Swing program. Save as QuantumSnake.java
 */
public class QuantumSnake extends JPanel implements ActionListener {
    // Window + grid
    static final int WINDOW_SIZE = 700;
    static final int CELL = 20;
    static final int COLS = WINDOW_SIZE / CELL;
    static final int ROWS = WINDOW_SIZE / CELL;
    static final int TIMER_MS = 90; // base speed

    // Snake
    LinkedList<Point> snake;
    enum Dir {UP, DOWN, LEFT, RIGHT}
    Dir dir;
    boolean running = false;
    javax.swing.Timer timer;

    // Food (moving)
    Point food;
    int foodVx = 0, foodVy = 0;
    Random rnd = new Random();

    // Portals
    Point portalA, portalB;

    // Power-ups (special cell that drops occasionally)
    Point rewindPU = null; // grant rewind
    boolean hasRewind = false;

    // Modules
    boolean magnetOwned = false;
    boolean phaseOwned = false;
    boolean magnetActive = false;
    boolean phaseActive = false;
    int magnetTimer = 0;
    int phaseTimer = 0;
    final int MODULE_DURATION = 50; // ticks (~50 * TIMER_MS)

    // Rewind history
    static class Snapshot {
        LinkedList<Point> snakeCopy;
        Dir dir;
        Point foodCopy;
        Point rewindPUcopy;
        boolean magnetOwned, phaseOwned, hasRewind;
        int magnetTimer, phaseTimer;
    }
    Deque<Snapshot> history = new ArrayDeque<>();
    final int HISTORY_CAP = 80; // ~80 ticks (~7s at 90ms) - we'll store less frequently
    boolean rewinding = false;

    // Score
    int score = 0;

    public QuantumSnake() {
        setPreferredSize(new Dimension(WINDOW_SIZE, WINDOW_SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        initGame();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int kc = e.getKeyCode();
                if (!running) {
                    if (kc == KeyEvent.VK_SPACE) initGame();
                    return;
                }
                switch (kc) {
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_W:
                        if (dir != Dir.DOWN) dir = Dir.UP;
                        break;
                    case KeyEvent.VK_DOWN:
                    case KeyEvent.VK_S:
                        if (dir != Dir.UP) dir = Dir.DOWN;
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_A:
                        if (dir != Dir.RIGHT) dir = Dir.LEFT;
                        break;
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_D:
                        if (dir != Dir.LEFT) dir = Dir.RIGHT;
                        break;
                    case KeyEvent.VK_M: activateMagnet(); break;
                    case KeyEvent.VK_P: activatePhase(); break;
                    case KeyEvent.VK_R: tryRewind(); break;
                }
            }
        });

        timer = new javax.swing.Timer(TIMER_MS, this);
        timer.start();
    }

    private void initGame() {
        snake = new LinkedList<>();
        int sx = COLS/2, sy = ROWS/2;
        for (int i = 0; i < 5; i++) snake.add(new Point(sx - i, sy));
        dir = Dir.RIGHT;
        placeFood();
        placePortals();
        rewindPU = null;
        hasRewind = false;
        magnetOwned = false;
        phaseOwned = false;
        magnetActive = false;
        phaseActive = false;
        magnetTimer = 0;
        phaseTimer = 0;
        history.clear();
        rewinding = false;
        score = 0;
        running = true;
    }

    private void placeFood() {
        while (true) {
            int x = rnd.nextInt(COLS);
            int y = rnd.nextInt(ROWS);
            Point p = new Point(x,y);
            if (!snake.contains(p) && !isPortal(p)) { food = p; break; }
        }
        // pick random initial velocity - small chance to move
        if (rnd.nextDouble() < 0.7) { foodVx = 0; foodVy = 0; }
        else {
            int[] dx = {-1,0,1};
            foodVx = dx[rnd.nextInt(3)];
            foodVy = dx[rnd.nextInt(3)];
        }
    }

    private void placePortals() {
        portalA = randomEmptyCell();
        portalB = randomEmptyCell();
        // ensure not same and not on snake or food
        while (portalB.equals(portalA) || snake.contains(portalB) || portalB.equals(food)) portalB = randomEmptyCell();
    }

    private Point randomEmptyCell() {
        Point p;
        do {
            p = new Point(rnd.nextInt(COLS), rnd.nextInt(ROWS));
        } while (snake.contains(p) || (food != null && p.equals(food)));
        return p;
    }

    private boolean isPortal(Point p) {
        return p.equals(portalA) || p.equals(portalB);
    }

    private void pushSnapshot() {
        // keep compact snapshots; clone snake positions
        Snapshot s = new Snapshot();
        s.snakeCopy = new LinkedList<>();
        for (Point q : snake) s.snakeCopy.add(new Point(q.x, q.y));
        s.dir = dir;
        s.foodCopy = new Point(food.x, food.y);
        s.rewindPUcopy = (rewindPU==null?null:new Point(rewindPU.x, rewindPU.y));
        s.magnetOwned = magnetOwned; s.phaseOwned = phaseOwned; s.hasRewind = hasRewind;
        s.magnetTimer = magnetTimer; s.phaseTimer = phaseTimer;
        history.addLast(s);
        if (history.size() > HISTORY_CAP) history.removeFirst();
    }

    private void tryRewind() {
        if (!hasRewind || rewinding || history.isEmpty()) return;
        rewinding = true;
        hasRewind = false;
    }

    private void activateMagnet() {
        if (!magnetOwned || magnetActive) return;
        magnetOwned = false; magnetActive = true; magnetTimer = MODULE_DURATION;
    }

    private void activatePhase() {
        if (!phaseOwned || phaseActive) return;
        phaseOwned = false; phaseActive = true; phaseTimer = MODULE_DURATION;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !rewinding) {
            // periodically store snapshots (every tick)
            pushSnapshot();
            gameTick();
        } else if (rewinding) {
            rewindTick();
        }
        repaint();
    }

    private void gameTick() {
        // modules timers
        if (magnetActive) {
            magnetTimer--;
            if (magnetTimer <= 0) magnetActive = false;
        }
        if (phaseActive) {
            phaseTimer--;
            if (phaseTimer <= 0) phaseActive = false;
        }

        // moving food behavior: flee if snake head near, else wander
        Point head = snake.getFirst();
        int dx = food.x - head.x;
        int dy = food.y - head.y;
        int distMan = Math.abs(dx) + Math.abs(dy);

        if (magnetActive) {
            // magnet pulls food toward head
            if (food.x < head.x) food.x++;
            else if (food.x > head.x) food.x--;
            if (food.y < head.y) food.y++;
            else if (food.y > head.y) food.y--;
        } else {
            // if close, food flees away
            if (distMan <= 5 && (foodVx != 0 || foodVy != 0 || rnd.nextDouble() < 0.7)) {
                // choose direction away from head
                if (Math.abs(dx) >= Math.abs(dy)) {
                    foodVx = dx > 0 ? 1 : -1;
                    foodVy = (rnd.nextBoolean()?0:(dy>0?1:-1));
                } else {
                    foodVy = dy > 0 ? 1 : -1;
                    foodVx = (rnd.nextBoolean()?0:(dx>0?1:-1));
                }
                // invert to flee
                foodVx = -foodVx;
                foodVy = -foodVy;
            } else {
                // small random wander occasionally
                if (rnd.nextDouble() < 0.12) {
                    int[] vals = {-1,0,1};
                    foodVx = vals[rnd.nextInt(3)];
                    foodVy = vals[rnd.nextInt(3)];
                }
            }

            // apply velocity (keep in bounds)
            int nx = food.x + foodVx;
            int ny = food.y + foodVy;
            if (nx >= 0 && nx < COLS) food.x = nx;
            if (ny >= 0 && ny < ROWS) food.y = ny;
        }

        // occasional power-up spawn (rewind or modules)
        if (rewindPU == null && rnd.nextDouble() < 0.005) {
            // spawn a random power-up: either rewind or a module
            rewindPU = randomEmptyCell();
        }

        // move snake
        Point newHead = new Point(head.x, head.y);
        switch (dir) {
            case UP: newHead.y -= 1; break;
            case DOWN: newHead.y += 1; break;
            case LEFT: newHead.x -= 1; break;
            case RIGHT: newHead.x += 1; break;
        }

        // portal teleport
        if (newHead.equals(portalA)) {
            newHead = new Point(portalB.x, portalB.y);
        } else if (newHead.equals(portalB)) {
            newHead = new Point(portalA.x, portalA.y);
        }

        // wall collision -> end
        if (newHead.x < 0 || newHead.x >= COLS || newHead.y < 0 || newHead.y >= ROWS) {
            running = false; return;
        }

        // self-collision: allowed if phaseActive
        boolean selfHit = snake.contains(newHead);
        if (selfHit && !phaseActive) { running = false; return; }

        // add head
        snake.addFirst(newHead);

        // eat food?
        if (newHead.equals(food)) {
            score += 10;
            placeFood();
            // 20% chance to grant random module or rewind
            double p = rnd.nextDouble();
            if (p < 0.20) {
                // grant a random module or rewind
                double q = rnd.nextDouble();
                if (q < 0.33) { magnetOwned = true; }
                else if (q < 0.66) { phaseOwned = true; }
                else { hasRewind = true; }
            }
        } else {
            // move forward (remove tail) normally
            snake.removeLast();
        }

        // collect power-up if on it
        if (rewindPU != null && newHead.equals(rewindPU)) {
            // randomly give either rewind, magnet, or phase
            double r = rnd.nextDouble();
            if (r < 0.33) hasRewind = true;
            else if (r < 0.66) magnetOwned = true;
            else phaseOwned = true;
            rewindPU = null;
        }

        // occasionally move portals slightly (novelty)
        if (rnd.nextDouble() < 0.002) {
            portalA = randomEmptyCell();
            portalB = randomEmptyCell();
            while (portalB.equals(portalA)) portalB = randomEmptyCell();
        }
    }

    private void rewindTick() {
        // pop a few snapshots per tick for visible fast rewind
        int pops = 2;
        for (int i = 0; i < pops && !history.isEmpty(); i++) {
            Snapshot s = history.removeLast();
            snake = new LinkedList<>();
            for (Point p : s.snakeCopy) snake.add(new Point(p.x, p.y));
            dir = s.dir;
            food = new Point(s.foodCopy.x, s.foodCopy.y);
            rewindPU = (s.rewindPUcopy==null?null:new Point(s.rewindPUcopy.x, s.rewindPUcopy.y));
            magnetOwned = s.magnetOwned; phaseOwned = s.phaseOwned; hasRewind = s.hasRewind;
            magnetTimer = s.magnetTimer; phaseTimer = s.phaseTimer;
        }
        // stop rewinding when history low or empty
        if (history.size() < 4 || history.isEmpty()) rewinding = false;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // background grid
        g.setColor(new Color(12,12,18));
        g.fillRect(0,0,getWidth(),getHeight());

        // draw grid lines lightly
        g.setColor(new Color(30,30,40));
        for (int i = 0; i <= COLS; i++) g.drawLine(i*CELL,0,i*CELL,ROWS*CELL);
        for (int j = 0; j <= ROWS; j++) g.drawLine(0,j*CELL,COLS*CELL,j*CELL);

        // draw portals
        drawCell(g, portalA.x, portalA.y, new Color(180,80,220));
        drawCell(g, portalB.x, portalB.y, new Color(220,140,40));

        // draw food (moving)
        drawCell(g, food.x, food.y, Color.RED);
        // draw a small arrow showing movement
        if (foodVx!=0 || foodVy!=0) {
            int cx = food.x*CELL + CELL/2, cy = food.y*CELL + CELL/2;
            g.setColor(Color.PINK);
            g.drawLine(cx, cy, cx + foodVx*6, cy + foodVy*6);
        }

        // draw power-up if present
        if (rewindPU != null) {
            drawCell(g, rewindPU.x, rewindPU.y, Color.CYAN);
            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString("PU", rewindPU.x*CELL+6, rewindPU.y*CELL + 14);
        }

        // draw snake
        boolean head = true;
        for (Point p : snake) {
            Color c = head ? (phaseActive ? Color.MAGENTA : Color.GREEN.brighter()) : Color.GREEN.darker();
            drawCell(g, p.x, p.y, c);
            head = false;
        }

        // HUD
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("Score: " + score, 8, 16);
        g.drawString("Magnet (M): " + (magnetOwned? "OWNED" : magnetActive? "ACTIVE":"-"), 8, 36);
        g.drawString("Phase  (P): " + (phaseOwned? "OWNED" : phaseActive? "ACTIVE":"-"), 8, 56);
        g.drawString("Rewind (R): " + (hasRewind? "READY":"-"), 8, 76);
        g.drawString("Portals: A<->B", 8, 96);
        g.drawString("Controls: Arrow/WASD, M, P, R, SPACE to restart", 8, 116);

        if (!running) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("SansSerif", Font.BOLD, 32));
            String s = "GAME OVER - Press SPACE to restart";
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(s);
            g.drawString(s, (getWidth()-w)/2, getHeight()/2);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            String s2 = "Final Score: "+score;
            g.drawString(s2, (getWidth()-g.getFontMetrics().stringWidth(s2))/2, getHeight()/2 + 40);
        }

        if (rewinding) {
            g.setColor(Color.ORANGE);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            String s = "REWINDING...";
            g.drawString(s, getWidth()/2 - 60, 24);
        }
    }

    private void drawCell(Graphics2D g, int cx, int cy, Color c) {
        int x = cx*CELL, y = cy*CELL;
        g.setColor(c);
        g.fillRect(x+1, y+1, CELL-2, CELL-2);
        g.setColor(c.darker());
        g.drawRect(x+1, y+1, CELL-2, CELL-2);
    }

    private static void createAndShowGUI() {
        JFrame f = new JFrame("Quantum Snake — portals, modules & rewind");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        QuantumSnake p = new QuantumSnake();
        f.setContentPane(p);
        f.pack();
        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(QuantumSnake::createAndShowGUI);
    }
}
