package dev.vstbridge.android.runtime;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winlator.core.FileUtils;
import com.winlator.core.TarCompressorUtils;
import com.winlator.xenvironment.RootFS;
import com.winlator.xenvironment.RootFSInstaller;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/** Installs the embedded Wine/Box64 runtime without exposing Winlator's UI. */
public final class RuntimeSetupActivity extends Activity {
    private TextView status;
    private ProgressBar progress;
    private Button action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildScreen());
        beginSetup();
    }

    private View buildScreen() {
        int pad = dp(24);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(12, 18, 24));

        TextView eyebrow = text("VST BRIDGE RUNTIME", 12, Color.rgb(105, 230, 179));
        eyebrow.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(eyebrow);

        TextView title = text("Preparing Windows audio", 30, Color.rgb(234, 240, 246));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title, margins(0, 8, 0, 12));

        root.addView(text(
                "VST Bridge is installing its private Wine and Box64 environment. No Winlator container setup is required.",
                16, Color.rgb(154, 169, 184)), margins(0, 0, 0, 24));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(true);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(10)));

        status = text("Checking runtime…", 15, Color.rgb(234, 240, 246));
        root.addView(status, margins(0, 16, 0, 20));

        action = new Button(this);
        action.setText("Done");
        action.setAllCaps(false);
        action.setVisibility(View.GONE);
        action.setOnClickListener(view -> finish());
        root.addView(action);

        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        page.setBackgroundColor(Color.rgb(12, 18, 24));
        page.addView(root, new ScrollView.LayoutParams(-1, -2));
        return page;
    }

    private void beginSetup() {
        RootFS rootFS = RootFS.find(this);
        if (rootFS.isValid() && rootFS.getVersion() >= RootFSInstaller.LATEST_VERSION) {
            status.setText("Creating the VST Bridge environment…");
            createContainer();
            return;
        }

        progress.setIndeterminate(false);
        status.setText("Installing Wine, Box64, and system files…");
        Executors.newSingleThreadExecutor().execute(() -> installRootFS(rootFS));
    }

    private void installRootFS(RootFS rootFS) {
        File rootDir = rootFS.getRootDir();
        if (rootDir.exists()) FileUtils.delete(rootDir);
        if (!rootDir.mkdirs() && !rootDir.isDirectory()) {
            fail("Could not create the runtime directory.");
            return;
        }

        long length = TarCompressorUtils.getContentLength(
                TarCompressorUtils.Type.ZSTD, this, RootFSInstaller.FILENAME, rootDir);
        AtomicLong extracted = new AtomicLong();
        boolean success = TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD, this, RootFSInstaller.FILENAME, rootDir, (file, size) -> {
                    if (size > 0 && length > 0) {
                        int value = (int) Math.min(100, extracted.addAndGet(size) * 100 / length);
                        runOnUiThread(() -> progress.setProgress(value));
                    }
                    return file;
                });

        if (!success) {
            fail("The runtime files could not be installed. Check free storage and try again.");
            return;
        }
        rootFS.createRFSVersionFile(RootFSInstaller.LATEST_VERSION);
        runOnUiThread(this::createContainer);
    }

    private void createContainer() {
        progress.setIndeterminate(true);
        status.setText("Creating the private VST Bridge Wine environment…");
        VstRuntimeManager.ensureContainer(this, (container, error) -> {
            if (container == null) {
                fail(error == null ? "Could not create the Wine environment." : error);
                return;
            }
            progress.setIndeterminate(false);
            progress.setProgress(100);
            status.setText("Runtime ready. You can now open an imported plug-in editor.");
            action.setText("Done");
            action.setOnClickListener(view -> finish());
            action.setVisibility(View.VISIBLE);
        });
    }

    private void fail(String message) {
        runOnUiThread(() -> {
            progress.setIndeterminate(false);
            progress.setProgress(0);
            status.setText(message);
            action.setText("Try again");
            action.setOnClickListener(view -> beginSetup());
            action.setVisibility(View.VISIBLE);
        });
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
