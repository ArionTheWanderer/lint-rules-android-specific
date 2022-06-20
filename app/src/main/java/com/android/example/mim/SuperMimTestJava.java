package com.android.example.mim;


public class SuperMimTestJava {
    protected MyDataStructure myDataStructure = new MyDataStructure();
    private String first;
    private int second;
    protected String superString = "superString";
    private int superFunVar = 3;

    int staticCandidateFun() {
        return 2;
    }

    void staticCandidateFun2() {
        System.out.println("wqewq");
    }

    SuperMimTestJava(String first, int second) {
        this.first = first;
        this.second = second;
    }

    int firstFunJava() {
        myDataStructure.test();
        return 3;
    }

    int localWlFun() {
        MyDataStructure localMds = new MyDataStructure();
        return localMds.firstInt;
    }

    public int secondFunJava() {
        MyDataStructure localMds = new MyDataStructure();
        second += 1;
        return second;
    }

    protected int superFun() {
        return superFunVar;
    }

    class MyDataStructure {
        public int firstInt = 2;
        public String secondString = "ds";

        public int test(){
            return 2;
        }
    }
}
