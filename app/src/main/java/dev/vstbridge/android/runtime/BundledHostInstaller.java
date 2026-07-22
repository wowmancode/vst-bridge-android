package dev.vstbridge.android.runtime;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BundledHostInstaller {
    private static final String ASSET_PATH = "runtime/windows-x86_64/vst-bridge-host.exe";

    private BundledHostInstaller() {}

    public static File install(Context context) throws IOException {
        File directory = new File(context.getFilesDir(), "runtime/windows-x86_64");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Could not create the runtime payload directory");
        }

        File destination = new File(directory, "vst-bridge-host.exe");
        if (destination.isFile() && destination.length() > 0) return destination;
        File temporary = new File(directory, "vst-bridge-host.exe.tmp");
        try (InputStream input = context.getAssets().open(ASSET_PATH);
             FileOutputStream output = new FileOutputStream(temporary)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } catch (IOException error) {
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
            throw new IOException("Could not extract " + ASSET_PATH + ": " + error.getClass().getSimpleName() + ": " + error.getMessage(), error);
        }

        if (destination.exists() && !destination.delete()) {
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
            throw new IOException("Could not replace the Windows host payload");
        }
        if (!temporary.renameTo(destination)) {
            //noinspection ResultOfMethodCallIgnored
            temporary.delete();
            throw new IOException("Could not install the Windows host payload");
        }
        return destination;
    }
}
