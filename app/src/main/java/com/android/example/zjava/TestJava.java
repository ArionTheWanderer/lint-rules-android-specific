package com.android.example.zjava;

import java.lang.System;
import java.util.HashMap;

public class TestJava extends SuperTestJava {
    private double third;

    public static int staticInt = 4;

    public String thirdFun(String param) {
        return param + "";
    }

    public int dotFunJava(SuperTestJava superTestJava) {
        superTestJava.myDataStructure.secondString.toLowerCase();
        superTestJava.superString.toLowerCase().charAt(2);
        System.out.println();
        return myDataStructure.firstInt;
    }


    public int dotFunJava2(SuperTestJava superTestJava) {
        myDataStructure.secondString.toLowerCase();
        return myDataStructure.firstInt;
    }

    TestJava(String firstTest, int secondTest) {
        super(firstTest, secondTest);
    }

    TestJava(String firstTest, int secondTest, double third) {
        this(firstTest, secondTest);
        this.third = third;
        HashMap   < Integer, Object > hm = new HashMap<>();
    }

//    public void firstFunJava() {
//        String superStringLocal = superString;
//        String newString = superString + "New";
//        this.third += 1.0D;
//        super.firstFunJava();
//    }

    public int secondFunJava() {
        int superInt = superFun();
        int superReturned = super.secondFunJava();
        return superReturned + superInt + 2;
    }

    public static void staticFunc() {
    }
}
