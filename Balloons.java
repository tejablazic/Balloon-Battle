import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

//Glavni razred igre Balloon Battle
public class Balloons {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Balloon Battle");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Okno na začetku nastavi na maksimalno velikost
        frame.setMinimumSize(new Dimension(800, 1000)); // Minimalna velikost okna
        frame.setResizable(true); // Spreminjanje velikosti okna
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout()); // Razporeditev vsebine okna

        StartMenu startMenu = new StartMenu(frame);
        frame.add(startMenu, BorderLayout.CENTER);

        frame.validate(); // Preverjanje in posodobitev postavitve vsebine
        frame.repaint();
        frame.setVisible(true);
    }
}

//Razred za začetni meni
class StartMenu extends JPanel {
    private JFrame frame;
    private JButton playButton; // Gumb za igranje
    
    // Konstruktor za StartMenu
    public StartMenu(JFrame frame) {
        this.frame = frame;
        this.setLayout(null);

        Font buttonFont = new Font("Arial", Font.BOLD, 20);

        playButton = new JButton("Play");
        playButton.setFont(buttonFont);
        playButton.setBounds(frame.getWidth() / 2 - 75, frame.getHeight() / 2 + 50, 150, 50);
        playButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });

        this.add(playButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Nadrejena metoda za risanje
        Color lightBlue = new Color(51, 153, 255);
        g.setColor(lightBlue);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        g.drawString("Balloon Battle!", getWidth() / 2 - 175, getHeight() / 2 - 25);
        playButton.setBounds(getWidth() / 2 - 75, getHeight() / 2 + 50, 150, 50);
    }

    private void startGame() {
        frame.getContentPane().removeAll(); // Odstrani vse komponente iz vsebine okna
        Panel2 panel = new Panel2(); // Ustvari nov igralni panel
        frame.add(panel, BorderLayout.CENTER);

        JPanel south = new JPanel(); // Ustvari nov panel za spodnji rob
        south.setPreferredSize(new Dimension(south.getWidth(), 30));
        Color darkBlue = new Color(0, 111, 160);
        south.setBackground(darkBlue);
        frame.add(south, BorderLayout.SOUTH);

        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        panel.requestFocusInWindow();

        new Thread(new GameLoop(panel, frame)).start();
        frame.revalidate();
        frame.repaint();
    }
}

//Razred za igralno zanko
class GameLoop implements Runnable {
    private Panel2 panel;
    private JFrame frame;
    private int balloonCounter = 0; // Števec balonov
    private double fallSpeedMultiplier = 1.0; // Hitrost padanja
    private boolean gameOver = false;
    
    // Konstruktor za GameLoop
    public GameLoop(Panel2 panel, JFrame frame) {
        this.panel = panel;
        this.frame = frame;
    }

    @Override
    public void run() {
        runGameLoop();  // Zagon igralne zanke
    }

    public void runGameLoop() {
        int cas = 0;
        int naslednji = 0;
        Random random = new Random(); // Ustvari objekt za generiranje naključnih števil
        gameOver = false;
        balloonCounter = 0;
        fallSpeedMultiplier = 1.0;

        while (!gameOver) { // Igra se izvaja
            if (cas == naslednji) {
                cas = 0;
                naslednji = 30;
                BufferedImage slika = panel.osnovniBaloni.get(random.nextInt(panel.osnovniBaloni.size())); // Naključna slika balona
                int x = random.nextInt(panel.getWidth() - 64); // Naključni položaj
                Balloon balon = new Balloon(slika, x, 0, fallSpeedMultiplier); // Ustvari nov balon
                panel.baloni.add(balon);
                balloonCounter++;
                if (balloonCounter % 20 == 0) { // Če je število balonov večkratnik 20, poveča hitrost padanja
                    fallSpeedMultiplier *= 1.1;
                }
            }

            for (Balloon balon : panel.baloni) {
                balon.updatePosition(); // Posodobi položaj balona
                if (balon.y >= panel.getHeight() - 60) { // Če balon doseže dno, se igra konča
                    gameOver = true;
                }
            }

            List<Ball> ballsToRemove = new ArrayList<>(); // Seznam za odstranjevanje krogel
            for (Ball ball : panel.balls) {
                ball.updatePosition(); // Posodobi položaj krogle
                if (ball.y <= 0) { // Če krogla doseže vrh zaslona, jo doda na seznam za odstranjevanje
                    ballsToRemove.add(ball);
                }
            }
            panel.balls.removeAll(ballsToRemove); // Odstrani krogle na seznamu

            panel.checkCollisions(); // Preveri trke med kroglami in baloni

            frame.repaint();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cas++;
        }

        panel.setGameOver(true);
        frame.repaint();
    }
}

//Razred za igralni panel
@SuppressWarnings("serial")
class Panel2 extends JPanel implements KeyListener {
    int polozaj;
    BufferedImage ladja;
    BufferedImage background;
    List<BufferedImage> osnovniBaloni;
    List<Balloon> baloni;
    List<Ball> balls;
    private boolean gameOver = false;
    private JButton retryButton;
    private GameLoop gameLoop;  // Referenca na igralno zanko
    private int score = 0;
    
    // Konstruktor za Panel2
    public Panel2() {
        osnovniBaloni = new ArrayList<>();
        baloni = new ArrayList<>();
        balls = new ArrayList<>();
        polozaj = 500;
        try { // Uvoz slik
            ladja = ImageIO.read(new File("resources/pirate ship.png"));
            osnovniBaloni.add(ImageIO.read(new File("resources/blue balloon.png")));
            osnovniBaloni.add(ImageIO.read(new File("resources/green balloon.png")));
            osnovniBaloni.add(ImageIO.read(new File("resources/pink balloon.png")));
            osnovniBaloni.add(ImageIO.read(new File("resources/red balloon.png")));
            osnovniBaloni.add(ImageIO.read(new File("resources/yellow balloon.png")));
            background = ImageIO.read(new File("resources/background.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        addKeyListener(this);
        setFocusable(true);

        retryButton = new JButton("Retry");
        retryButton.setFont(new Font("Arial", Font.BOLD, 20));
        retryButton.setBounds(getWidth() / 2 - 75, getHeight() / 2 + 50, 150, 50);
        retryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
                score = 0;
            }
        });
        retryButton.setVisible(false); // Skrije gumb za ponovno igranje
        this.setLayout(null);
        this.add(retryButton);
    }
    
    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        retryButton.setVisible(gameOver); // Prikaže gumb za ponovno igranje
    }

    public void resetGame() {
        gameOver = false;
        retryButton.setVisible(false); // Skrije gumb za ponovno igranje
        baloni.clear();
        balls.clear();
        new Thread(new GameLoop(this, (JFrame) SwingUtilities.getWindowAncestor(this))).start(); // Zažene novo igralno zanko
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null); // Nariše ozadje
        }
        if (gameOver) { // Igra končana
            g.setColor(Color.RED);
            g.setFont(new Font("Arial", Font.BOLD, 50));
            g.drawString("GAME OVER", getWidth() / 2 - 155, getHeight() / 2 - 100);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.drawString("Score: " + score, getWidth() / 2 - 80, getHeight() / 2 - 15);
            retryButton.setBounds(getWidth() / 2 - 75, getHeight() / 2 + 50, 150, 50);
        } else { // Igra ni končana
            for (Balloon balon : baloni) {
                g.drawImage(balon.slika, balon.x, balon.y, 64, 90, null);
            }
            g.setColor(Color.WHITE);
            for (Ball ball : balls) {
                g.fillOval(ball.x, ball.y, 10, 10);
            }
            g.drawImage(ladja, polozaj, getHeight() - 100, 100, 100, null);
            drawScore(g);
        }
    }
    
    public void drawScore(Graphics g) {
        g.setColor(Color.WHITE);        
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("Score: " + score, 20, 40);
    }
    
    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!gameOver) { // Igra ni končana
            int tipka = e.getKeyCode(); // Dobi kodo pritisnjene tipke, nato se ustrezno premakne
            if (tipka == KeyEvent.VK_LEFT) {
                polozaj = Math.max(polozaj - 15, 0);
                repaint();
            } else if (tipka == KeyEvent.VK_RIGHT) {
                polozaj = Math.min(polozaj + 15, getWidth() - 100);
                repaint();
            } else if (tipka == KeyEvent.VK_SPACE) {
                shootBall();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void shootBall() {
        balls.add(new Ball(polozaj + 45, getHeight() - 110)); // Doda kroglo na pozicijo ladje
    }

    public void checkCollisions() { // Preverjanje trkov med kroglami in baloni
        List<Balloon> balloonsToRemove = new ArrayList<>();
        List<Ball> ballsToRemove = new ArrayList<>();

        for (Balloon balloon : baloni) {
            for (Ball ball : balls) {
                if (ball.x > balloon.x && ball.x < balloon.x + 64 && ball.y > balloon.y && ball.y < balloon.y + 90) { // Trk
                    balloonsToRemove.add(balloon);
                    ballsToRemove.add(ball);
                    score++; // Poveča rezultat
                }
            }
        }
        baloni.removeAll(balloonsToRemove);
        balls.removeAll(ballsToRemove);
    }
}

// Razred za kroglo
class Ball {
    int x;
    int y;
    private static final int SPEED = 10; // Hitrost krogle
    
    // Konstruktor za inicializacijo položaja krogle
    public Ball(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void updatePosition() { // Premikanje navzgor
        y -= SPEED; // Zmanjša y koordinato za hitrost, da se krogla premika navzgor
    }
}

// Razred za balon
class Balloon {
    BufferedImage slika;
    int x;
    int y;
    private double fallSpeedMultiplier; // Množitelj hitrosti padanja
    private static final int BASE_FALL_SPEED = 1; // Osnovna hitrost padanja

    // Konstruktor za inicializacijo položaja in hitrosti padanja balona
    public Balloon(BufferedImage slika, int x, int y, double fallSpeedMultiplier) {
        this.slika = slika;
        this.x = x;
        this.y = y;
        this.fallSpeedMultiplier = fallSpeedMultiplier;
    }

    public void updatePosition() { // Premikanje navzdol
        y += BASE_FALL_SPEED * fallSpeedMultiplier; // Poveča Y koordinato za osnovno hitrost * množitelj, da se balon premika navzdol
    }
}
