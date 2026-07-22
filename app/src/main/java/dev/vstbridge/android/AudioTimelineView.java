package dev.vstbridge.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** Lightweight native waveform/timeline beside the isolated Windows editor. */
public final class AudioTimelineView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float playhead;

    public AudioTimelineView(Context context, AttributeSet attributes) {
        super(context, attributes);
        paint.setStrokeWidth(context.getResources().getDisplayMetrics().density);
    }

    public void setPlayhead(float fraction) {
        playhead = Math.max(0f, Math.min(1f, fraction));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        canvas.drawColor(Color.rgb(14, 21, 29));
        paint.setColor(Color.rgb(45, 60, 73));
        for (int i = 0; i <= 8; i++) canvas.drawLine(width * i / 8f, 0, width * i / 8f, height, paint);
        for (int i = 1; i < 4; i++) canvas.drawLine(0, height * i / 4f, width, height * i / 4f, paint);

        paint.setColor(Color.rgb(105, 230, 179));
        paint.setStrokeWidth(2f * getResources().getDisplayMetrics().density);
        float center = height / 2f;
        float previousX = 0;
        float previousY = center;
        for (int x = 0; x < width; x += 4) {
            double envelope = 0.25 + 0.65 * Math.abs(Math.sin(x * 0.013));
            float y = center + (float) (Math.sin(x * 0.091) * envelope * height * 0.34);
            canvas.drawLine(previousX, previousY, x, y, paint);
            previousX = x;
            previousY = y;
        }
        paint.setColor(Color.rgb(255, 178, 73));
        canvas.drawLine(playhead * width, 0, playhead * width, height, paint);
    }
}
