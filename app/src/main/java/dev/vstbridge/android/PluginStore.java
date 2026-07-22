package dev.vstbridge.android;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class PluginStore {
    private static final String PREFS = "plugin_library";
    private static final String KEY = "plugins";
    private final Context context;

    PluginStore(Context context) {
        this.context = context.getApplicationContext();
    }

    List<PluginRecord> load() {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]");
        List<PluginRecord> plugins = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                PluginRecord plugin = PluginRecord.fromJson(array.getJSONObject(i));
                if (new File(plugin.path).isFile()) plugins.add(plugin);
            }
        } catch (JSONException ignored) {
            // A corrupt index should not make the app unusable. Imported files stay intact.
        }
        Collections.sort(plugins, (left, right) -> Long.compare(right.importedAt, left.importedAt));
        return plugins;
    }

    PluginRecord importPlugin(Uri uri) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        String displayName = displayName(resolver, uri);
        String lower = displayName.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".dll") && !lower.endsWith(".vst3")) {
            throw new IOException("Choose a Windows .dll or .vst3 plug-in file");
        }

        File directory = new File(context.getFilesDir(), "plugins");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create the plug-in library");
        }

        String id = UUID.randomUUID().toString();
        File destination = new File(directory, id + "-" + safeName(displayName));
        long copied = 0;
        try (InputStream input = resolver.openInputStream(uri);
             FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("Could not open the selected file");
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                copied += count;
            }
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            destination.delete();
            throw error;
        }

        PeInspector.Architecture architecture = PeInspector.inspect(destination);
        if (architecture == PeInspector.Architecture.NOT_PE) {
            //noinspection ResultOfMethodCallIgnored
            destination.delete();
            throw new IOException("The selected file is not a valid Windows plug-in binary");
        }

        PluginRecord plugin = new PluginRecord(
                id, displayName, destination.getAbsolutePath(), copied, System.currentTimeMillis());
        List<PluginRecord> plugins = load();
        plugins.add(0, plugin);
        save(plugins);
        return plugin;
    }

    void remove(PluginRecord plugin) {
        List<PluginRecord> plugins = load();
        plugins.removeIf(candidate -> candidate.id.equals(plugin.id));
        save(plugins);
        //noinspection ResultOfMethodCallIgnored
        new File(plugin.path).delete();
    }

    private void save(List<PluginRecord> plugins) {
        JSONArray array = new JSONArray();
        for (PluginRecord plugin : plugins) {
            try {
                array.put(plugin.toJson());
            } catch (JSONException ignored) {
                // All record fields are JSON-safe primitives.
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY, array.toString()).apply();
    }

    private static String displayName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        }
        String segment = uri.getLastPathSegment();
        return segment == null ? "plugin.dll" : segment;
    }

    private static String safeName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
