package io.github.barmoury.asynchronous;

public class Threading {

    public static void run(Runnable target) {
        new Thread(target).start();
    }

}
