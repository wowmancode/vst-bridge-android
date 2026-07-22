package dev.vstbridge.android.runtime;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.winlator.XServerDisplayActivity;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.FileUtils;
import com.winlator.xenvironment.RootFS;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

/** Maps VST Bridge payloads into a Winlator container and starts its Wine/X server. */
public final class WinlatorRuntimeBridge implements RuntimeBridge {
    private final Context context;
    private final File hostPayload;

    public WinlatorRuntimeBridge(Context context) {
        this.context = context.getApplicationContext();
        File installed = null;
        try {
            installed = BundledHostInstaller.install(context);
        } catch (IOException ignored) {
        }
        hostPayload = installed;
    }

    @Override
    public State state() {
        if (!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a")) {
            return State.UNSUPPORTED_DEVICE;
        }
        if (hostPayload == null || !RootFS.find(context).isValid()) {
            return State.NOT_INSTALLED;
        }
        return firstContainer() == null ? State.NOT_INSTALLED : State.READY;
    }

    @Override
    public String statusMessage() {
        if (!Arrays.asList(Build.SUPPORTED_ABIS).contains("arm64-v8a")) {
            return "This runtime requires a 64-bit ARM Android device.";
        }
        if (hostPayload == null) {
            return "This APK does not contain the Windows host. Install the runtime APK artifact.";
        }
        if (!RootFS.find(context).isValid()) {
            return "Runtime files are bundled but not installed. Tap Set up runtime.";
        }
        if (firstContainer() == null) {
            return "Create a container in Runtime setup, then return here.";
        }
        return "Wine, Box64, X server, audio bridge, and Windows host are ready.";
    }

    public static void openSetup(Context context) {
        context.startActivity(new Intent(context, com.winlator.MainActivity.class));
    }

    @Override
    public void launch(LaunchRequest request) {
        if (state() != State.READY) throw new IllegalStateException(statusMessage());

        Container container = firstContainer();
        if (container == null) throw new IllegalStateException("No Winlator container is available.");
        File guestDir = new File(container.getRootDir(), ".wine/drive_c/vstbridge");
        if (!guestDir.isDirectory() && !guestDir.mkdirs()) {
            throw new IllegalStateException("Could not create the VST directory in the Wine container.");
        }

        File guestHost = new File(guestDir, "vst-bridge-host.exe");
        String extension = request.pluginFile.getName().toLowerCase(Locale.ROOT).endsWith(".vst3")
                ? ".vst3" : ".dll";
        File guestPlugin = new File(guestDir, "plugin-" + safeId(request.pluginId) + extension);
        if (!FileUtils.copy(hostPayload, guestHost) || !FileUtils.copy(request.pluginFile, guestPlugin)) {
            throw new IllegalStateException("Could not copy the host and plug-in into the Wine container.");
        }

        String command = guestHost.getAbsolutePath()
                + " editor " + safeId(request.pluginId)
                + " C:\\vstbridge\\" + guestPlugin.getName();
        Intent intent = new Intent(context, XServerDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("container_id", container.id);
        intent.putExtra("exec_path", command);
        context.startActivity(intent);
    }

    private Container firstContainer() {
        if (!RootFS.find(context).isValid()) return null;
        ContainerManager manager = new ContainerManager(context);
        return manager.getContainers().isEmpty() ? null : manager.getContainers().get(0);
    }

    private static String safeId(String value) {
        String result = value.replaceAll("[^A-Za-z0-9_-]", "_");
        return result.isEmpty() ? "plugin" : result;
    }
}
