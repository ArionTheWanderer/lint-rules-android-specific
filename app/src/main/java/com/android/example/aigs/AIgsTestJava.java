package com.android.example.aigs;

public class AIgsTestJava {
    private String firstJava = "heh";
    private int secondJava = 2;

    public void setFirstJava(String firstJava) {
        this.firstJava = firstJava;
        this.firstJava += firstJava;
    }

    public void setSecondJava(int secondJava) {
        this.secondJava = secondJava;
    }

    public void voidFunJava() {
        secondJava += 1;
    }

    public String stringFunJava() {
        return firstJava;
    }
}
