package io.github.barmoury.asynchronous;

public class Threading {

    public void run(Runnable target) {
        new Thread(target).start();
    }

}
