package com.andrew_lowman.workouttimer.Model;

public interface modelService {
    void startStopwatch();
    void startCountdownTimer();
    void restartStopwatch(long time);
    void restartCountdownTimer();
    void pauseStopwatch();
    void pauseCountdownTimer();
    void cancelStopwatch();
    void cancelCountdownTimer();
}
