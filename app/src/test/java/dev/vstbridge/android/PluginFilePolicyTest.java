package dev.vstbridge.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PluginFilePolicyTest {
    @Test
    public void acceptsDllRegardlessOfCase() {
        assertTrue(PluginFilePolicy.isSupportedFilename("synth.dll"));
        assertTrue(PluginFilePolicy.isSupportedFilename("SYNTH.DLL"));
    }

    @Test
    public void acceptsVst3RegardlessOfCase() {
        assertTrue(PluginFilePolicy.isSupportedFilename("effect.vst3"));
        assertTrue(PluginFilePolicy.isSupportedFilename("EFFECT.VST3"));
    }

    @Test
    public void rejectsUnrelatedAndMisleadingNames() {
        assertFalse(PluginFilePolicy.isSupportedFilename("plugin.dll.zip"));
        assertFalse(PluginFilePolicy.isSupportedFilename("plugin.exe"));
        assertFalse(PluginFilePolicy.isSupportedFilename(null));
    }
}
