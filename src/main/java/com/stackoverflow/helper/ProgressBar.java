package com.stackoverflow.helper;

public class ProgressBar {
    private int current;
    private int max;
    private long time = 0;

    public ProgressBar(int current, int max) {
        this.current = current;
        this.max = max;
    }

    public ProgressBar(int max) {
        this(0, max);
    }

    public ProgressBar() { this(0, 0); }

    public void start() {
        this.time = System.nanoTime();
    }

    public void end() {
        System.out.print("\n");
    }

    public void reset(int current, int max) {
        time = 0;
        this.current = current;
        this.max = max;
    }

    public void reset(int max) {
        this.reset(0, max);
    }

    public void set(int value) {
        this.current = value;
    }

    public void next() {
        ++this.current;
    }

    public long elapsed() {
        return System.nanoTime() - this.time;
    }

    public double ETA() {
        float p = this.percentage();
        if(p == 0) {
            return 100.0 * this.elapsed();
        }
        return ((100.0 / this.percentage()) - 1) * this.elapsed();
    }

    public float percentage() {
        return (float)(this.current) / (float)this.max * 100;
    }

    private String formatTime(double time) {
        double seconds = time / (1000*1000*1000);
        long minutes = (long)Math.floor(seconds / 60);
        seconds %= 60;
        long hours = (long)Math.floor((float)minutes / 60);
        minutes %= 60;

        return String.format("%4d:%02d:%05.2f", hours, minutes, seconds);
    }

    public void print() {
        System.out.print("\r\tProgress: " + String.format("%3.2f", this.percentage()) + "% (" + this.current + " of " +
                this.max + "); " + this.formatTime(this.elapsed()) + " | ETA: " + this.formatTime(this.ETA()));
    }
}
