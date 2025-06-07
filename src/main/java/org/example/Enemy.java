package org.example;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Enemy {
    public Circle shape;
    public PVector velocity;
    public int health;
    public int maxHealth;

    public Enemy(double x, double y, int health, double speed) {
        this.health = health;
        this.maxHealth = health;
        this.shape = new Circle(x, y, 15, Color.CRIMSON); // radius=15

        // initial velocity is zero; we’ll re‐compute toward player each frame
        this.velocity = new PVector(0, 0, 0);
        this.shape.setUserData(this);  // so we can retrieve Enemy from the Node if needed
    }

    /**
     * Recompute velocity to point from current position toward (px,py),
     * at the given speed.
     */
    public void updateVelocityToward(double px, double py, double speed) {
        double dx = px - shape.getCenterX();
        double dy = py - shape.getCenterY();
        double len = Math.hypot(dx, dy);
        if (len > 0) {
            velocity.x = (float)(dx / len * speed);
            velocity.y = (float)(dy / len * speed);
        } else {
            velocity.x = velocity.y = 0;
        }
    }

    /** Move the enemy by its current velocity. */
    public void move() {
        shape.setCenterX(shape.getCenterX() + velocity.x);
        shape.setCenterY(shape.getCenterY() + velocity.y);
    }
}

