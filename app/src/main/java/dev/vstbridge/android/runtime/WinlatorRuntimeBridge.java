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

/** Maps VST Bridge payloads into its private Wine container and starts its Wine/X server. */
public final class WinlatorRuntimeBridge implements RuntimeBridge {
    private final Context context;
    private final File hostPayload;
    private final String hostInstallError;

    public WinlatorRuntimeBridge(Context context) {
        this.context = context.getApplicationContext();
        File installed = null;
        String installError = null;
        try {
            installed = BundledHostInstaller.install(context);
        } catch (IOException error) {
            installError = error.getMessage();
        }
        hostPayload = installed;
        hostInstallError = installError;
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
            return "Windows host installation failed: "
                    + (hostInstallError == null ? "bundled asset unavailable" : hostInstallError);
        }
        if (!RootFS.find(context).isValid()) {
            return "Runtime files are bundled but not installed. Tap Set up runtime.";
        }
        if (firstContainer() == null) {
            return "The private Wine environment is not ready. Tap Set up runtime.";
        }
        return "Wine, Box64, X server, audio bridge, and Windows host are ready.";
    }

    public static void openSetup(Context context) {
        context.startActivity(new Intent(context, RuntimeSetupActivity.class));
    }

    @Override
    public void launch(LaunchRequest request) {
        if (state() != State.READY) throw new IllegalStateException(statusMessage());

        Container container = firstContainer();
        if (container == null) throw new IllegalStateException("The VST Bridge Wine environment is unavailable.");
        new ContainerManager(context).activateContainer(container);
        File activeHome = new File(RootFS.find(context).getRootDir(), RootFS.HOME_PATH);
        File guestDir = new File(activeHome, ".wine/drive_c/vstbridge");
        if (!guestDir.isDirectory() && !guestDir.mkdirs()) {
            throw new IllegalStateException("Could not create the VST directory in the Wine container.");
        }

        File guestHost = new File(guestDir, "vst-bridge-host.exe");
        String extension = request.pluginFile.getName().toLowerCase(Locale.ROOT).endsWith(".vst3")
                ? ".vst3" : ".dll";
        File guestPlugin = new File(guestDir, "plugin-" + safeId(request.pluginId) + extension);
        if (!FileUtils.copy(hostPayload, guestHost) || guestHost.length() != hostPayload.length()) {
            throw new IllegalStateException("Windows host copy failed: expected " + hostPayload.length()
                    + " bytes but found " + guestHost.length() + " at " + guestHost.getPath());
        }
        if (!FileUtils.copy(request.pluginFile, guestPlugin) || guestPlugin.length() != request.pluginFile.length()) {
            throw new IllegalStateException("Plug-in copy failed: expected " + request.pluginFile.length()
                    + " bytes but found " + guestPlugin.length() + ".");
        }

        String arguments = "editor " + safeId(request.pluginId)
                + " C:\\vstbridge\\" + guestPlugin.getName();
        Intent intent = new Intent(context, XServerDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("container_id", container.id);
        intent.putExtra("vst_bridge_session", true);
        intent.putExtra("plugin_name", request.pluginFile.getName());
        intent.putExtra("exec_file", guestHost.getAbsolutePath());
        intent.putExtra("exec_args", arguments);
        intent.putExtra("expected_host_size", guestHost.length());
        context.startActivity(intent);
    }

    private Container firstContainer() {
        if (!RootFS.find(context).isValid()) return null;
        return VstRuntimeManager.findContainer(context);
    }

    private static String safeId(String value) {
        String result = value.replaceAll("[^A-Za-z0-9_-]", "_");
        return result.isEmpty() ? "plugin" : result;
    }
}
