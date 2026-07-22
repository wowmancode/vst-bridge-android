package dev.vstbridge.android;

import org.json.JSONException;
import org.json.JSONObject;

final class PluginRecord {
    final String id;
    final String name;
    final String path;
    final long size;
    final long importedAt;

    PluginRecord(String id, String name, String path, long size, long importedAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.size = size;
        this.importedAt = importedAt;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("name", name)
                .put("path", path)
                .put("size", size)
                .put("importedAt", importedAt);
    }

    static PluginRecord fromJson(JSONObject json) throws JSONException {
        return new PluginRecord(
                json.getString("id"),
                json.getString("name"),
                json.getString("path"),
                json.getLong("size"),
                json.getLong("importedAt")
        );
    }
}
