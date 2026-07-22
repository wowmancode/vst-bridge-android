package dev.vstbridge.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.winlator.BuildConfig;

import dev.vstbridge.android.runtime.WinlatorRuntimeBridge;
import dev.vstbridge.android.runtime.RuntimeBridge;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int OPEN_PLUGIN = 100;
    private static final int OPEN_AUDIO = 101;
    private PluginStore store;
    private RuntimeBridge runtime;
    private TextView runtimeStatus;
    private LinearLayout pluginList;
    private TextView emptyState;
    private TextView audioStatus;
    private AudioTimelineView audioTimeline;
    private MediaPlayer audioPlayer;
    private final Handler transportHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new PluginStore(this);
        runtime = new WinlatorRuntimeBridge(this);
        setContentView(buildScreen());
        renderPlugins();
    }

    private View buildScreen() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(12, 18, 24));

        TextView eyebrow = text("WINDOWS AUDIO ON ANDROID", 12, Color.rgb(105, 230, 179));
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(eyebrow);

        TextView title = text("VST Bridge " + BuildConfig.VERSION_NAME, 34, Color.rgb(234, 240, 246));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, margins(-1, -2, 0, 0, 0, 12));

        TextView intro = text(
                "Load an audio clip, import a Windows VST2 plug-in, and open its editor beside the native Android timeline.",
                16, Color.rgb(154, 169, 184));
        root.addView(intro, margins(-1, -2, 0, 0, 0, 16));

        LinearLayout session = panel();
        TextView sessionTitle = text("Audio session", 18, Color.rgb(234, 240, 246));
        sessionTitle.setTypeface(Typeface.DEFAULT_BOLD);
        session.addView(sessionTitle);
        audioStatus = text("No audio clip loaded • dry preview", 13, Color.rgb(154, 169, 184));
        session.addView(audioStatus, margins(-1, -2, 0, 2, 0, 8));
        audioTimeline = new AudioTimelineView(this, null);
        session.addView(audioTimeline, margins(-1, 110, 0, 0, 0, 8));
        LinearLayout transport = new LinearLayout(this);
        Button importAudio = smallButton("Load audio");
        importAudio.setOnClickListener(view -> chooseAudio());
        Button playAudio = smallButton("Play");
        playAudio.setOnClickListener(view -> playAudio());
        Button stopAudio = smallButton("Stop");
        stopAudio.setOnClickListener(view -> stopAudio());
        transport.addView(importAudio);
        transport.addView(playAudio);
        transport.addView(stopAudio);
        session.addView(transport);
        session.addView(text("Audio preview is native Android. The loaded Windows VST editor opens in the right-side panel; real-time VST processing is still experimental.", 12, Color.rgb(154, 169, 184)));
        root.addView(session, margins(-1, -2, 0, 0, 0, 12));

        LinearLayout runtimeCard = panel();
        TextView runtimeTitle = text("Runtime", 13, Color.rgb(154, 169, 184));
        runtimeTitle.setTypeface(Typeface.DEFAULT_BOLD);
        runtimeCard.addView(runtimeTitle);
        runtimeStatus = text(runtime.statusMessage(), 15, Color.rgb(234, 240, 246));
        runtimeCard.addView(runtimeStatus, margins(-1, -2, 0, 4, 0, 8));
        Button setupRuntime = smallButton("Set up runtime");
        setupRuntime.setOnClickListener(view -> WinlatorRuntimeBridge.openSetup(this));
        runtimeCard.addView(setupRuntime);
        root.addView(runtimeCard, margins(-1, -2, 0, 0, 0, 16));

        Button importButton = new Button(this);
        importButton.setText("Import plug-in");
        importButton.setTextSize(16);
        importButton.setAllCaps(false);
        importButton.setTextColor(Color.rgb(8, 31, 24));
        importButton.setBackgroundColor(Color.rgb(105, 230, 179));
        importButton.setOnClickListener(view -> choosePlugin());
        root.addView(importButton, margins(-1, 52, 0, 0, 0, 22));

        TextView libraryTitle = text("Plug-in library", 20, Color.rgb(234, 240, 246));
        libraryTitle.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(libraryTitle);

        emptyState = text("No plug-ins imported yet.", 15, Color.rgb(154, 169, 184));
        root.addView(emptyState, margins(-1, -2, 0, 12, 0, 0));

        ScrollView scroll = new ScrollView(this);
        pluginList = new LinearLayout(this);
        pluginList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(pluginList);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (runtimeStatus != null) {
            runtime = new WinlatorRuntimeBridge(this);
            runtimeStatus.setText(runtime.statusMessage());
            renderPlugins();
        }
    }

    private void chooseAudio() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, OPEN_AUDIO);
    }

    private void playAudio() {
        if (audioPlayer == null) {
            Toast.makeText(this, "Load an audio clip first", Toast.LENGTH_SHORT).show();
            return;
        }
        audioPlayer.start();
        audioStatus.setText("Playing dry audio preview");
        updatePlayhead();
    }

    private void stopAudio() {
        if (audioPlayer != null) {
            audioPlayer.pause();
            audioPlayer.seekTo(0);
        }
        transportHandler.removeCallbacksAndMessages(null);
        if (audioTimeline != null) audioTimeline.setPlayhead(0f);
        if (audioStatus != null) audioStatus.setText("Audio ready • dry preview");
    }

    private void updatePlayhead() {
        if (audioPlayer == null || !audioPlayer.isPlaying()) return;
        int duration = audioPlayer.getDuration();
        audioTimeline.setPlayhead(duration > 0 ? audioPlayer.getCurrentPosition() / (float) duration : 0f);
        transportHandler.postDelayed(this::updatePlayhead, 50);
    }

    private void choosePlugin() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_PLUGIN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        if (requestCode == OPEN_AUDIO) {
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {}
            getSharedPreferences("vstbridge", MODE_PRIVATE).edit().putString("audioUri", uri.toString()).apply();
            if (audioPlayer != null) audioPlayer.release();
            audioPlayer = MediaPlayer.create(this, uri);
            if (audioPlayer == null) {
                audioStatus.setText("Could not load this audio file");
            } else {
                audioStatus.setText("Audio ready • dry preview");
                audioPlayer.setOnCompletionListener(player -> stopAudio());
            }
            return;
        }
        if (requestCode != OPEN_PLUGIN) return;
        try {
            store.importPlugin(uri);
            renderPlugins();
            Toast.makeText(this, "Plug-in imported", Toast.LENGTH_SHORT).show();
        } catch (IOException error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPlugins() {
        List<PluginRecord> plugins = store.load();
        pluginList.removeAllViews();
        emptyState.setVisibility(plugins.isEmpty() ? View.VISIBLE : View.GONE);
        for (PluginRecord plugin : plugins) pluginList.addView(pluginCard(plugin));
    }

    private View pluginCard(PluginRecord plugin) {
        LinearLayout card = panel();
        TextView name = text(plugin.name, 17, Color.rgb(234, 240, 246));
        name.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(name);
        PeInspector.Architecture architecture = PeInspector.inspect(new File(plugin.path));
        String detail = String.format(Locale.US, "%.1f MB  •  %s",
                plugin.size / 1048576.0,
                plugin.name.toLowerCase(Locale.ROOT).endsWith(".vst3") ? "VST3" : "VST2");
        detail += "  •  " + architecture.label;
        card.addView(text(detail, 13, Color.rgb(154, 169, 184)));

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.END);
        Button remove = smallButton("Remove");
        remove.setOnClickListener(view -> {
            store.remove(plugin);
            renderPlugins();
        });
        Button open = smallButton("Open editor");
        open.setEnabled(true);
        open.setOnClickListener(view -> {
            try {
                if (runtime.state() != RuntimeBridge.State.READY) {
                    throw new IllegalStateException(runtime.statusMessage());
                }
                if (!architecture.supported) {
                    throw new IllegalStateException("This plug-in is " + architecture.label
                            + ". Editor hosting currently requires an x86-64 Windows DLL.");
                }
                if (plugin.name.toLowerCase(Locale.ROOT).endsWith(".vst3")) {
                    throw new IllegalStateException("VST3 editor hosting is not implemented yet. Use an x86-64 VST2 .dll.");
                }
                runtime.launch(new dev.vstbridge.android.runtime.LaunchRequest(
                        plugin.id, new File(plugin.path), 48000, 256, true));
            } catch (RuntimeException error) {
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        actions.addView(remove);
        actions.addView(open, margins(-2, -2, 8, 0, 0, 0));
        card.addView(actions, margins(-1, -2, 0, 10, 0, 0));
        return card;
    }



    @Override
    protected void onDestroy() {
        transportHandler.removeCallbacksAndMessages(null);
        if (audioPlayer != null) audioPlayer.release();
        super.onDestroy();
    }

    private LinearLayout panel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(14), dp(16), dp(14));
        panel.setBackgroundColor(Color.rgb(23, 33, 43));
        panel.setLayoutParams(margins(-1, -2, 0, 0, 0, 10));
        return panel;
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        return button;
    }

    private TextView text(String value, int sp, int color) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setLineSpacing(0, 1.12f);
        return text;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int left, int top, int right, int bottom) {
        int resolvedWidth = width < 0 ? width : dp(width);
        int resolvedHeight = height < 0 ? height : dp(height);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(resolvedWidth, resolvedHeight);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
