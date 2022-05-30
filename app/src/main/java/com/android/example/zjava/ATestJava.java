package com.android.example.zjava;

public class ATestJava extends SuperTestJava {
    private double third;

    public int dotFunJava(SuperTestJava superTestJava) {
        superTestJava.myDataStructure.secondString.toLowerCase();
        return myDataStructure.firstInt;
    }

    public int dotFunJava2(SuperTestJava superTestJava) {
        myDataStructure.secondString.toLowerCase();
        return myDataStructure.firstInt;
    }

    ATestJava(String firstTest, int secondTest) {
        super(firstTest, secondTest);
    }

    ATestJava(String firstTest, int secondTest, double third) {
        this(firstTest, secondTest);
        this.third = third;
    }

    public void firstFunJava() {
        String superStringLocal = superString;
        String newString = superString + "New";
        this.third += 1.0D;
        super.firstFunJava();
    }

    public int secondFunJava() {
        int superInt = superFun();
        int superReturned = super.secondFunJava();
        return superReturned + superInt + 2;
    }

    public String thirdFun(String param) {
        return param + "";
    }

    public static void staticFunc() {
    }
}
