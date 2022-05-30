package com.android.example.zjava;

public class SuperTestJava {
    private String first;
    private int second;
    protected String superString = "superString";
    private int superFunVar = 3;
    protected MyDataStructure myDataStructure = new MyDataStructure();

    SuperTestJava(String first, int second) {
        this.first = first;
        this.second = second;
    }

    public void firstFunJava(){}

    public int secondFunJava() {
        second += 1;
        return second;
    }

    protected int superFun() {
        return superFunVar;
    }

    class MyDataStructure {
        public int firstInt = 2;
        public String secondString = "ds";
    }
}
