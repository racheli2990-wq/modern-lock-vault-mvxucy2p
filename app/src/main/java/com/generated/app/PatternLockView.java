package com.generated.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PatternLockView extends View {

    public interface OnPatternListener {
        void onPatternDetected(List<Integer> pattern);
    }

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectedDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final List<Integer> selectedNodes = new ArrayList<>();
    private float currentX = -1;
    private float currentY = -1;
    private boolean isDrawing = false;
    private OnPatternListener listener;

    private static final int GRID_SIZE = 3;
    private float[][] nodePositions = new float[9][2];
    private float hitRadius = 0;

    public PatternLockView(Context context) {
        super(context);
        init();
    }

    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setOnPatternListener(OnPatternListener listener) {
        this.listener = listener;
    }

    private void init() {
        dotPaint.setColor(Color.parseColor("#80FFFFFF"));
        dotPaint.setStyle(Paint.Style.FILL);

        selectedDotPaint.setColor(Color.parseColor("#3B82F6"));
        selectedDotPaint.setStyle(Paint.Style.FILL);

        linePaint.setColor(Color.parseColor("#993B82F6"));
        linePaint.setStrokeWidth(12f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float stepX = (float) w / (GRID_SIZE + 1);
        float stepY = (float) h / (GRID_SIZE + 1);
        hitRadius = Math.min(stepX, stepY) * 0.45f;

        int index = 0;
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                nodePositions[index][0] = stepX * (c + 1);
                nodePositions[index][1] = stepY * (r + 1);
                index++;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw connections
        if (selectedNodes.size() > 1) {
            for (int i = 0; i < selectedNodes.size() - 1; i++) {
                int from = selectedNodes.get(i);
                int to = selectedNodes.get(i + 1);
                canvas.drawLine(nodePositions[from][0], nodePositions[from][1],
                        nodePositions[to][0], nodePositions[to][1], linePaint);
            }
        }

        // Draw line to current touch point
        if (isDrawing && !selectedNodes.isEmpty() && currentX >= 0) {
            int last = selectedNodes.get(selectedNodes.size() - 1);
            canvas.drawLine(nodePositions[last][0], nodePositions[last][1], currentX, currentY, linePaint);
        }

        // Draw dots
        for (int i = 0; i < 9; i++) {
            float x = nodePositions[i][0];
            float y = nodePositions[i][1];
            if (selectedNodes.contains(i)) {
                canvas.drawCircle(x, y, 22f, selectedDotPaint);
                // Outer glow ring
                Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                ringPaint.setStyle(Paint.Style.STROKE);
                ringPaint.setStrokeWidth(5f);
                ringPaint.setColor(Color.parseColor("#4D3B82F6"));
                canvas.drawCircle(x, y, 40f, ringPaint);
            } else {
                canvas.drawCircle(x, y, 14f, dotPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                resetPattern();
                isDrawing = true;
                checkNodeHit(x, y);
                currentX = x;
                currentY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                currentX = x;
                currentY = y;
                checkNodeHit(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDrawing = false;
                currentX = -1;
                currentY = -1;
                invalidate();
                if (listener != null && !selectedNodes.isEmpty()) {
                    listener.onPatternDetected(new ArrayList<>(selectedNodes));
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void checkNodeHit(float x, float y) {
        for (int i = 0; i < 9; i++) {
            float dx = x - nodePositions[i][0];
            float dy = y - nodePositions[i][1];
            if ((dx * dx + dy * dy) <= (hitRadius * hitRadius)) {
                if (!selectedNodes.contains(i)) {
                    selectedNodes.add(i);
                    vibrateQuick();
                    break;
                }
            }
        }
    }

    private void vibrateQuick() {
        try {
            Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                v.vibrate(20);
            }
        } catch (Exception ignored) {
        }
    }

    public void resetPattern() {
        selectedNodes.clear();
        invalidate();
    }
}
