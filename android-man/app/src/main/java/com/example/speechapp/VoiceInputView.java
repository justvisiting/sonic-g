package com.example.speechapp;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.util.Random;

public class VoiceInputView extends FrameLayout {
    private View voiceAnimationContainer;
    private View[] voiceBars;
    private ValueAnimator[] animators;
    private Random random;
    private TextView statusText;
    private TextView tapText;

    public enum VoiceStatus {
        LISTENING("Listening..."),
        PROCESSING("Processing..."),
        ERROR("Couldn't hear that");

        private final String text;

        VoiceStatus(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
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
        LayoutInflater.from(getContext()).inflate(R.layout.voice_input_view, this, true);
        voiceAnimationContainer = findViewById(R.id.voiceAnimationContainer);
        statusText = findViewById(R.id.voiceStatusText);
        tapText = findViewById(R.id.voiceTapText);
        
        // Initialize voice bars
        voiceBars = new View[]{
            findViewById(R.id.voiceBar1),
            findViewById(R.id.voiceBar2),
            findViewById(R.id.voiceBar3),
            findViewById(R.id.voiceBar4),
            findViewById(R.id.voiceBar5)
        };

        animators = new ValueAnimator[voiceBars.length];
        random = new Random();
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
    }

    public void setStatus(VoiceStatus status) {
        statusText.setText(status.getText());
        
        // Update animations based on status
        switch (status) {
            case LISTENING:
                statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
                tapText.setVisibility(View.VISIBLE);
                for (View bar : voiceBars) {
                    bar.setVisibility(View.VISIBLE);
                }
                break;
            case PROCESSING:
                statusText.setTextColor(getResources().getColor(android.R.color.darker_gray));
                tapText.setVisibility(View.GONE);
                for (View bar : voiceBars) {
                    bar.setVisibility(View.GONE);
                }
                break;
            case ERROR:
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                tapText.setVisibility(View.VISIBLE);
                tapText.setText("Tap microphone to try again");
                for (View bar : voiceBars) {
                    bar.setVisibility(View.GONE);
                }
                break;
        }
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

        animator.setStartDelay(random.nextInt(200)); // Random start delay 0-200ms
        animator.start();
        animators[index] = animator;
    }

    public void updateWithAmplitude(float amplitude) {
        // Scale amplitude to a reasonable range (0.3 - 1.0)
        float scaledAmplitude = 0.3f + (amplitude * 0.7f);
        for (View bar : voiceBars) {
            bar.setScaleY(scaledAmplitude);
        }
    }
}
