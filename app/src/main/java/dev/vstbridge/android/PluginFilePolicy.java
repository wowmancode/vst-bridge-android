package dev.vstbridge.android;

import java.util.Locale;

final class PluginFilePolicy {
    private PluginFilePolicy() {}

    static boolean isSupportedFilename(String displayName) {
        if (displayName == null) return false;
        String lower = displayName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".dll") || lower.endsWith(".vst3");
    }
}
