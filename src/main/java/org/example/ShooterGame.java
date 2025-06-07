package org.example;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import java.util.*;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class ShooterGame extends Application {
    private Pane playfield;
    private List<Walker> allWalkers = new ArrayList<>();
    private final List<Bullet> bullets = new LinkedList<>();
    private final List<Enemy> enemies = new ArrayList<>();

    private static final double BULLET_RADIUS = 5;
    private static final double BULLET_SPEED = 8;
    private static final double ENEMY_SPEED = 1;
    private static final int ENEMY_HEALTH = 3;
    private static final long SPAWN_INTERVAL_NS = 3_000_000_000L;

    private PVector mouse = new PVector(0,0,0);
    private long lastSpawnTime = 0;

    private boolean gameOver = false;
    private AnimationTimer loop;           // make it a field
    private Pane uiOverlay;                // for Game Over screen
    private int score = 0;
    private Label scoreLabel;


    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        StackPane layerPane = new StackPane();
        playfield = new Pane();
        layerPane.getChildren().add(playfield);
        root.setCenter(layerPane);

        Scene scene = new Scene(root, Settings.SCENE_WIDTH, Settings.SCENE_HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();

        scoreLabel = new Label("Score: 0");
        scoreLabel.setFont(Font.font(20));
        scoreLabel.setTextFill(Color.BLACK);
        // put it in a pane that lives in the top of the BorderPane
        StackPane topPane = new StackPane(scoreLabel);
        topPane.setAlignment(Pos.TOP_LEFT);
        //topPane.setPadding(new Insets(10));
        root.setTop(topPane);

        addWalker();

        // initialize spawn timer and spawn one enemy
        lastSpawnTime = System.nanoTime() - SPAWN_INTERVAL_NS;

        scene.addEventFilter(MouseEvent.ANY, e -> mouse.set(e.getX(), e.getY(), 0));
        scene.setOnMouseClicked(e -> shoot());


        // Prepare an overlay Pane (initially invisible)
        uiOverlay = new Pane();
        uiOverlay.setMouseTransparent(true);
        root.getChildren().add(uiOverlay);

        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (gameOver) return;

                // Walkers
                allWalkers.forEach(w -> w.step(mouse));
                allWalkers.forEach(Walker::checkBoundaries);
                allWalkers.forEach(Walker::display);

                // Bullets update
                updateBullets();

                // Spawn logic
                if (now - lastSpawnTime >= SPAWN_INTERVAL_NS) {
                    spawnEnemyRandomEdge();
                    lastSpawnTime = now;
                }

                // Enemies update
                updateEnemies();

                Walker pw = allWalkers.get(0);
                PVector ppos = pw.getLocation();
                for (Enemy e : enemies) {
                    double ex = e.shape.getCenterX(), ey = e.shape.getCenterY();
                    if (Math.hypot(ex - ppos.x, ey - ppos.y) < e.shape.getRadius() + pw.getRadius()) {
                        triggerGameOver();
                        break;
                    }
                }
            }
        };
        loop.start();

        scene.setOnKeyPressed(ev -> {
            if (gameOver && ev.getCode() == KeyCode.ENTER) {
                restartGame();
            }
        });

        primaryStage.show();
    }

    private void addWalker() {
        Walker walker = new Walker();
        allWalkers.add(walker);
        playfield.getChildren().add(walker);
    }

    private void shoot() {
        if (allWalkers.isEmpty()) return;
        Walker w = allWalkers.get(0);
        PVector vel = w.getVelocity();
        if (vel.x == 0 && vel.y == 0) return;
        PVector dir = vel.copy().normalize().mult(BULLET_SPEED);
        PVector pos = w.getLocation();
        Bullet b = new Bullet(pos.x, pos.y, dir.x, dir.y);
        bullets.add(b);
        playfield.getChildren().add(b.shape);
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            b.shape.setCenterX(b.shape.getCenterX() + b.dx);
            b.shape.setCenterY(b.shape.getCenterY() + b.dy);
            if (b.shape.getCenterX() < -BULLET_RADIUS ||
                    b.shape.getCenterX() > playfield.getWidth() + BULLET_RADIUS ||
                    b.shape.getCenterY() < -BULLET_RADIUS ||
                    b.shape.getCenterY() > playfield.getHeight() + BULLET_RADIUS) {
                playfield.getChildren().remove(b.shape);
                it.remove();
            }
        }
    }

    private void updateEnemies() {
        if (allWalkers.isEmpty()) return;
        PVector playerPos = allWalkers.get(0).getLocation();
        Iterator<Enemy> eit = enemies.iterator();
        while (eit.hasNext()) {
            Enemy e = eit.next();
            e.updateVelocityToward(playerPos.x, playerPos.y, ENEMY_SPEED);
            e.move();
            // collision with bullets
            Iterator<Bullet> bit = bullets.iterator();
            while (bit.hasNext()) {
                Bullet b = bit.next();
                if (e.shape.contains(b.shape.getCenterX(), b.shape.getCenterY())) {
                    bit.remove();
                    playfield.getChildren().remove(b.shape);
                    e.health--;
                    if (e.health <= 0) {
                        playfield.getChildren().remove(e.shape);
                        eit.remove();
                        score++;
                        scoreLabel.setText("Score: " + score);
                    } else {
                        // compute a fraction from 1.0 (full health) down to 0.0 (just hit)
                        double frac = (double)e.health / e.maxHealth;

                        // baseColor is the original crimson
                        Color base = Color.CRIMSON;

                        // build a new color with the same hue & brightness, but reduced saturation
                        // (so it “fades” toward pink/white as it takes damage)
                        Color faded = Color.hsb(
                                base.getHue(),
                                base.getSaturation() * frac,
                                base.getBrightness()
                        );

                        e.shape.setFill(faded);
                    }
                    break;
                }
            }
        }
    }

    private void spawnEnemyRandomEdge() {
        double w = Settings.SCENE_WIDTH;
        double h = Settings.SCENE_HEIGHT;
        double x, y;
        int edge = (int)(Math.random()*4);
        switch(edge) {
            case 0: x = Math.random()*w; y = -20; break;
            case 1: x = w+20; y = Math.random()*h; break;
            case 2: x = Math.random()*w; y = h+20; break;
            default: x = -20; y = Math.random()*h;
        }
        Enemy e = new Enemy(x,y,ENEMY_HEALTH,ENEMY_SPEED);
        enemies.add(e);
        playfield.getChildren().add(e.shape);
    }

    public static void main(String[] args) {
        launch(args);
    }

    /** Simple holder for each bullet’s shape + velocity. */
    private static class Bullet {
        final Circle shape;
        final double dx, dy;
        Bullet(double x, double y, double dx, double dy) {
            this.shape = new Circle(x, y, BULLET_RADIUS, Color.GOLD);
            this.dx = dx;
            this.dy = dy;
        }
    }

    private void triggerGameOver() {
        gameOver = true;
        loop.stop();

        // Show big "GAME OVER" text
        Text go = new Text("GAME OVER");
        go.setFont(Font.font(72));
        go.setFill(Color.RED);
        go.setStroke(Color.BLACK);
        go.setStrokeWidth(2);
        go.setX(Settings.SCENE_WIDTH/2 - go.getLayoutBounds().getWidth()/2);
        go.setY(Settings.SCENE_HEIGHT/2);

        Text prompt = new Text("Press ENTER to restart");
        prompt.setFont(Font.font(24));
        prompt.setFill(Color.WHITE);
        prompt.setX(Settings.SCENE_WIDTH/2 - prompt.getLayoutBounds().getWidth()/2);
        prompt.setY(Settings.SCENE_HEIGHT/2 + 50);

        uiOverlay.getChildren().setAll(go, prompt);
        uiOverlay.setVisible(true);
    }

    private void restartGame() {
        // Clear everything
        score = 0;
        scoreLabel.setText("Score: " + score);
        uiOverlay.getChildren().clear();
        uiOverlay.setVisible(false);
        playfield.getChildren().clear();
        allWalkers.clear();
        bullets.clear();
        enemies.clear();

        // Rebuild initial state
        addWalker();
        lastSpawnTime = System.nanoTime();
        gameOver = false;
        loop.start();
    }
}
