package dev.vstbridge.android.runtime;

import android.os.Build;

import java.util.Arrays;

public final class DevelopmentRuntimeBridge implements RuntimeBridge {
    @Override
    public State state() {
        boolean arm64 = Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a");
        return arm64 ? State.NOT_INSTALLED : State.UNSUPPORTED_DEVICE;
    }

    @Override
    public String statusMessage() {
        if (state() == State.UNSUPPORTED_DEVICE) {
            return "This prototype requires a 64-bit ARM Android device.";
        }
        return "Wine + Box64 runtime is not bundled in this build yet.";
    }

    @Override
    public void launch(LaunchRequest request) {
        throw new IllegalStateException(statusMessage());
    }
}
