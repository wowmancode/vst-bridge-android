package dev.vstbridge.android.runtime;


public interface RuntimeBridge {
    enum State { READY, NOT_INSTALLED, UNSUPPORTED_DEVICE }

    State state();

    String statusMessage();

    void launch(LaunchRequest request) throws RuntimeException;
}
