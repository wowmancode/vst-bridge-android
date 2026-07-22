package dev.vstbridge.android.runtime;

import android.content.Context;

import com.winlator.box64.Box64Preset;
import com.winlator.container.AudioDrivers;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.DXWrappers;
import com.winlator.container.GraphicsDrivers;

import org.json.JSONException;
import org.json.JSONObject;

/** Owns the private Wine container used by VST Bridge. */
public final class VstRuntimeManager {
    public static final String CONTAINER_NAME = "VST Bridge Runtime";
    private static final String EXTRA_OWNER = "vstBridgeRuntime";

    private VstRuntimeManager() {}

    public static Container findContainer(Context context) {
        for (Container container : new ContainerManager(context).getContainers()) {
            if ("1".equals(container.getExtra(EXTRA_OWNER))
                    || CONTAINER_NAME.equals(container.getName())) {
                return container;
            }
        }
        return null;
    }

    public static void ensureContainer(Context context, ResultCallback callback) {
        Container existing = findContainer(context);
        if (existing != null) {
            tagAndConfigure(existing);
            callback.onResult(existing, null);
            return;
        }

        JSONObject data = new JSONObject();
        try {
            data.put("name", CONTAINER_NAME);
            data.put("screenSize", Container.DEFAULT_SCREEN_SIZE);
            data.put("envVars", "MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=256MB WINEESYNC=1");
            data.put("graphicsDriver", GraphicsDrivers.VORTEK + "," + GraphicsDrivers.VIRGL);
            data.put("dxwrapper", DXWrappers.WINED3D);
            data.put("audioDriver", AudioDrivers.ALSA);
            data.put("wincomponents", Container.FALLBACK_WINCOMPONENTS);
            data.put("drives", "");
            data.put("startupSelection", Container.STARTUP_SELECTION_ESSENTIAL);
            data.put("box64Preset", Box64Preset.DEFAULT);
        } catch (JSONException error) {
            callback.onResult(null, error.getMessage());
            return;
        }

        new ContainerManager(context).createContainerAsync(data, container -> {
            if (container == null) {
                callback.onResult(null, "Could not create the VST Bridge Wine container.");
                return;
            }
            tagAndConfigure(container);
            callback.onResult(container, null);
        });
    }

    private static void tagAndConfigure(Container container) {
        container.setName(CONTAINER_NAME);
        container.setGraphicsDriver(GraphicsDrivers.VORTEK + "," + GraphicsDrivers.VIRGL);
        container.setDXWrapper(DXWrappers.WINED3D);
        container.setAudioDriver(AudioDrivers.ALSA);
        container.setWinComponents(Container.FALLBACK_WINCOMPONENTS);
        container.setDrives("");
        container.setStartupSelection(Container.STARTUP_SELECTION_ESSENTIAL);
        container.putExtra(EXTRA_OWNER, "1");
        container.saveData();
    }

    public interface ResultCallback {
        void onResult(Container container, String error);
    }
}
