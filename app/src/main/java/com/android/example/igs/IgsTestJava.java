package com.android.example.igs;

public class IgsTestJava {

    private String firstJava = "firstString";
    private int secondJava = 2;

    public String getFirstJava() {
        return firstJava;
    }

    public int getSecondJava() {
        System.out.println("qwerty");
        return secondJava;
    }

    public void setFirstJava(String firstJava) {
        this.firstJava = firstJava;
    }

    public void setSecondJava(int secondJava) {
        this.secondJava = secondJava;
        this.secondJava += secondJava;
    }

    public void gettersUsageJava() {
        System.out.println("qwerty");
        String firstJavaLocal = getFirstJava();
        int secondJavaLocal = getSecondJava();
    }

    public void settersUsageJava() {
        System.out.println("qwerty");
        setFirstJava("qwerty");
        setSecondJava(1337);
        int arg = 3;
        setSecondJava(arg);
    }

    public static void staticMethod() {
        System.out.println("static method");
    }
}
