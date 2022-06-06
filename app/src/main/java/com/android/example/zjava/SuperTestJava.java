package com.android.example.zjava;

public class SuperTestJava {
    protected MyDataStructure myDataStructure = new MyDataStructure();
    protected MyDataStructure myDataStructure2 = new MyDataStructure();
    private String first;
    private int second;
    protected String superString = "superString";
    private int superFunVar = 3;

    SuperTestJava(String first, int second) {
        this.first = first;
        this.second = second;
    }

    int firstFunJava() {
        myDataStructure.test();
        myDataStructure.acquire();
        return 3;
    }

    int localWlFun() {
        myDataStructure2.acquire();
        MyDataStructure localMds = new MyDataStructure();
        localMds.acquire();
        System.out.println("dsdsa");
        localMds.release();
        return localMds.firstInt;
    }

    public int secondFunJava() {
        MyDataStructure localMds = new MyDataStructure();
        localMds.acquire();
        myDataStructure.release();
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
        public void acquire() {}
        public void release() {}
    }
}
