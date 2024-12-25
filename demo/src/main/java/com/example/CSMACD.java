package com.example;

import java.util.Random;

public class CSMACD {
    public static final String JAM_SIGNAL = "!";
    public static final String PACKET_END = "E";

    public static boolean isBusy(){
        Random random = new Random();
        int probability = 7;
        int randomNumber = random.nextInt(10);
        return randomNumber < probability;
    }

    public static boolean isCollision(){
        Random random = new Random();
        int probability = 3;
        int randomNumber = random.nextInt(10);
        return randomNumber < probability;
    }

    public static void setDelay(int attempt){
        try {
            Thread.sleep(Math.min(attempt, 10) * 100L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
