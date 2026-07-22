package dev.vstbridge.android.runtime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public final class LaunchRequest {
    public static final int PROTOCOL_VERSION = 1;

    public final String pluginId;
    public final File pluginFile;
    public final int sampleRate;
    public final int blockSize;
    public final boolean openEditor;

    public LaunchRequest(
            String pluginId,
            File pluginFile,
            int sampleRate,
            int blockSize,
            boolean openEditor
    ) {
        if (pluginId == null || pluginId.isEmpty()) throw new IllegalArgumentException("pluginId");
        if (pluginFile == null || !pluginFile.isFile()) throw new IllegalArgumentException("pluginFile");
        if (sampleRate < 8000 || sampleRate > 384000) throw new IllegalArgumentException("sampleRate");
        if (blockSize < 16 || blockSize > 8192) throw new IllegalArgumentException("blockSize");
        this.pluginId = pluginId;
        this.pluginFile = pluginFile;
        this.sampleRate = sampleRate;
        this.blockSize = blockSize;
        this.openEditor = openEditor;
    }

    public JSONObject toJson() {
        try {
            return new JSONObject()
                    .put("protocolVersion", PROTOCOL_VERSION)
                    .put("command", "launch")
                    .put("pluginId", pluginId)
                    .put("pluginPath", pluginFile.getAbsolutePath())
                    .put("sampleRate", sampleRate)
                    .put("blockSize", blockSize)
                    .put("openEditor", openEditor);
        } catch (JSONException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
