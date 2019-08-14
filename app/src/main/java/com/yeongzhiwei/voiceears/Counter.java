package com.yeongzhiwei.voiceears;

class Counter {
    private int counter = 0;

    synchronized int increment() {
        counter++;
        return counter;
    }

    synchronized int decrement() {
        counter--;
        return counter;
    }
}
