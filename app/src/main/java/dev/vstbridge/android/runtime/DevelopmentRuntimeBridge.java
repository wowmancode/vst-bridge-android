package dev.vstbridge.android.runtime;

import android.content.Context;
import android.os.Build;

import java.io.IOException;
import java.util.Arrays;

public final class DevelopmentRuntimeBridge implements RuntimeBridge {
    private final boolean hostPayloadInstalled;

    public DevelopmentRuntimeBridge(Context context) {
        boolean installed;
        try {
            installed = BundledHostInstaller.install(context) != null;
        } catch (IOException ignored) {
            installed = false;
        }
        hostPayloadInstalled = installed;
    }

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
        if (hostPayloadInstalled) {
            return "Windows VST2/VST3 host is bundled. Wine + Box64 runtime is next.";
        }
        return "Windows host payload is not included in this APK build.";
    }

    @Override
    public void launch(LaunchRequest request) {
        throw new IllegalStateException(statusMessage());
    }
}
