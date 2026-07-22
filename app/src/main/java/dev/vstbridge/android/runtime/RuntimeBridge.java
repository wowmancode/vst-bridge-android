package dev.vstbridge.android.runtime;

import java.io.File;

public interface RuntimeBridge {
    enum State { READY, NOT_INSTALLED, UNSUPPORTED_DEVICE }

    State state();

    String statusMessage();

    void launch(File windowsPlugin) throws RuntimeException;
}
