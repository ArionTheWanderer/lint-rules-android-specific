package com.android.example.mim;

import java.lang.System;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MimTestJava extends SuperMimTestJava {
    private double third;

    public static int staticInt = 4;

    public String thirdFun(String param) {
        return param + "";
    }

    public int dotFunJava(SuperMimTestJava superMimTestJava) {
        superMimTestJava.myDataStructure.secondString.toLowerCase();
        superMimTestJava.superString.toLowerCase().charAt(2);
        System.out.println();
        return myDataStructure.firstInt;
    }


    public int dotFunJava2(SuperMimTestJava superMimTestJava) {
        myDataStructure.secondString.toLowerCase();
        return myDataStructure.firstInt;
    }

    MimTestJava(String firstTest, int secondTest) {
        super(firstTest, secondTest);
    }

    MimTestJava(String firstTest, int secondTest, double third) {
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

    public Map<Integer, List<String>> integerListMap() {
        return integerListMap();
    }

    public static void staticFunc() {
    }
}
