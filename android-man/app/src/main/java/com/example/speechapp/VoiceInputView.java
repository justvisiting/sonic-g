package com.example.speechapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import java.util.Random;

public class VoiceInputView extends FrameLayout {
    private View voiceAnimationContainer;
    private View[] voiceBars;
    private ValueAnimator[] animators;
    private Random random;
    private int currentColor = Color.parseColor("#4285F4"); // Google Blue

    public enum VoiceStatus {
        IDLE(Color.parseColor("#4285F4")), // Blue
        LISTENING(Color.parseColor("#34A853")), // Green
        PROCESSING(Color.parseColor("#34A853")), // Green
        ERROR(Color.parseColor("#EA4335")); // Red

        private final int color;

        VoiceStatus(int color) {
            this.color = color;
        }

        public int getColor() {
            return color;
        }
    }

    public VoiceInputView(Context context) {
        super(context);
        init();
    }

    public VoiceInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        LayoutInflater.from(getContext()).inflate(R.layout.voice_input_view, this, true);
        voiceAnimationContainer = findViewById(R.id.voiceAnimationContainer);
        
        // Initialize voice bars
        voiceBars = new View[]{
            findViewById(R.id.voiceBar1),
            findViewById(R.id.voiceBar2),
            findViewById(R.id.voiceBar3)
        };

        animators = new ValueAnimator[voiceBars.length];
        random = new Random();
        
        // Set initial state
        setStatus(VoiceStatus.IDLE);
        voiceAnimationContainer.setVisibility(View.GONE);
    }

    public void startAnimation() {
        voiceAnimationContainer.setVisibility(View.VISIBLE);
        setStatus(VoiceStatus.LISTENING);
        for (int i = 0; i < voiceBars.length; i++) {
            startBarAnimation(i);
        }
    }

    public void stopAnimation() {
        for (ValueAnimator animator : animators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        voiceAnimationContainer.setVisibility(View.GONE);
        setStatus(VoiceStatus.IDLE);
    }

    public void setStatus(VoiceStatus status) {
        currentColor = status.getColor();
        updateBackgroundColor(currentColor);
        
        switch (status) {
            case LISTENING:
                voiceAnimationContainer.setVisibility(View.VISIBLE);
                for (View bar : voiceBars) {
                    bar.setBackgroundColor(Color.WHITE);
                }
                break;
            case PROCESSING:
            case ERROR:
            case IDLE:
                voiceAnimationContainer.setVisibility(View.GONE);
                break;
        }
    }

    private void updateBackgroundColor(int color) {
        setBackgroundResource(R.drawable.circle_background);
        getBackground().setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void startBarAnimation(int index) {
        if (animators[index] != null) {
            animators[index].cancel();
        }

        View bar = voiceBars[index];
        float minScale = 0.3f;
        float maxScale = 1.0f;
        int duration = 600 + random.nextInt(400); // Random duration between 600-1000ms

        ValueAnimator animator = ValueAnimator.ofFloat(minScale, maxScale);
        animator.setDuration(duration);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            bar.setScaleY(scale);
        });

        animator.setStartDelay(index * 100); // Sequential delay for bars
        animator.start();
        animators[index] = animator;
    }

    public void updateWithAmplitude(float amplitude) {
        if (voiceAnimationContainer.getVisibility() == View.VISIBLE) {
            // Scale amplitude to a reasonable range (0.3 - 1.0)
            float scaledAmplitude = 0.3f + (amplitude * 0.7f);
            for (View bar : voiceBars) {
                bar.setScaleY(scaledAmplitude);
            }
        }
    }
}
