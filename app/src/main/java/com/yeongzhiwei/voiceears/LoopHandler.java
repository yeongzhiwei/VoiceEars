package com.yeongzhiwei.voiceears;

import android.os.Handler;

class LoopHandler {
    private Handler handler = new Handler();
    private int handlerDelay;
    private Runnable loopContent;

    LoopHandler(Runnable content) {
        this(200, content);
    }

    LoopHandler(int interval, Runnable content) {
        handlerDelay = interval;


        loopContent = () -> {
            try {
                content.run();
            } finally {
                handler.postDelayed(loopContent, handlerDelay);
            }
        };
    }

    void start() {
            loopContent.run();
    }

    void reset() {
        handler.removeCallbacks(loopContent);
        handler.postDelayed(loopContent, handlerDelay);
    }

    void stop() {
        handler.removeCallbacks(loopContent);
    }
}
