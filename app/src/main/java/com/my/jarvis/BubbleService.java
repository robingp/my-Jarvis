package com.my.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

public class BubbleService extends android.app.Service {

    private WindowManager wm;
    private View orb;
    private TextView caption;
    private WindowManager.LayoutParams orbParams, capParams;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private Brain brain;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private boolean listening = false;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        brain = new Brain(this);
        startForeground(1, buildNotification());
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.UK);
                tts.setPitch(0.85f);
                tts.setSpeechRate(0.98f);
            }
        });
        addOrb();
        addCaption();
    }

    private Notification buildNotification() {
        String id = "jarvis";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(id, "JARVIS", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, id)
                .setContentTitle("JARVIS is active")
                .setContentText("Tap the orb to speak. Tap here to manage.")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private int type() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void addOrb() {
        int size = (int) (64 * getResources().getDisplayMetrics().density);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#33e0ff"), Color.parseColor("#0066ff")});
        g.setShape(GradientDrawable.OVAL);
        g.setStroke((int) (3 * getResources().getDisplayMetrics().density), Color.parseColor("#88ffff"));

        TextView v = new TextView(this);
        v.setText("J");
        v.setTextColor(Color.WHITE);
        v.setTextSize(26);
        v.setGravity(Gravity.CENTER);
        v.setBackground(g);
        orb = v;

        orbParams = new WindowManager.LayoutParams(size, size, type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        orbParams.gravity = Gravity.TOP | Gravity.START;
        orbParams.x = 24;
        orbParams.y = 300;

        orb.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy, sx, sy; boolean moved;
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dx = orbParams.x; dy = orbParams.y; sx = e.getRawX(); sy = e.getRawY(); moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int nx = (int) (dx + e.getRawX() - sx), ny = (int) (dy + e.getRawY() - sy);
                        if (Math.abs(e.getRawX() - sx) > 12 || Math.abs(e.getRawY() - sy) > 12) moved = true;
                        orbParams.x = nx; orbParams.y = ny; wm.updateViewLayout(orb, orbParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggleListen();
                        return true;
                }
                return false;
            }
        });
        wm.addView(orb, orbParams);
    }

    private void addCaption() {
        caption = new TextView(this);
        caption.setTextColor(Color.parseColor("#bff0ff"));
        caption.setTextSize(15);
        caption.setPadding(28, 16, 28, 16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#cc001326"));
        bg.setCornerRadius(28);
        bg.setStroke(2, Color.parseColor("#3344e0ff"));
        caption.setBackground(bg);
        caption.setVisibility(View.GONE);

        capParams = new WindowManager.LayoutParams(
                (int) (260 * getResources().getDisplayMetrics().density),
                WindowManager.LayoutParams.WRAP_CONTENT, type(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT);
        capParams.gravity = Gravity.TOP | Gravity.START;
        capParams.x = 24; capParams.y = 380;
        wm.addView(caption, capParams);
    }

    private void show(String text) {
        ui.post(() -> {
            caption.setText(text);
            caption.setVisibility(View.VISIBLE);
            ui.removeCallbacks(hide);
            ui.postDelayed(hide, 6000);
        });
    }
    private final Runnable hide = () -> caption.setVisibility(View.GONE);

    private void speak(String text) {
        if (text == null || text.isEmpty()) return;
        show(text);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "j");
    }

    private void toggleListen() {
        if (listening) { stopListen(); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speak("Voice input is unavailable on this device.");
            return;
        }
        listening = true;
        orb.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { show("Listening..."); }
            public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                stopListen();
                if (r != null && !r.isEmpty()) process(r.get(0));
            }
            public void onError(int e) { stopListen(); if (e == SpeechRecognizer.ERROR_NO_MATCH) show("Didn't catch that."); }
            public void onBeginningOfSpeech() {}
            public void onBufferReceived(byte[] b) {}
            public void onEndOfSpeech() {}
            public void onPartialResults(Bundle b) {}
            public void onEvent(int i, Bundle b) {}
            public void onRmsChanged(float r) {}
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        recognizer.startListening(i);
    }

    private void stopListen() {
        listening = false;
        ui.post(() -> orb.animate().scaleX(1f).scaleY(1f).setDuration(200).start());
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
    }

    private void process(String said) {
        Brain.Result res = brain.handle(said);
        if (res.action != null) runAction(res.action, res.arg, res.reply);
        else speak(res.reply);
    }

    private void runAction(String action, String arg, String reply) {
        switch (action) {
            case "OPEN": openApp(arg, reply); break;
            case "FLASHLIGHT_ON": Actions.flashlight(this, true); speak(reply); break;
            case "FLASHLIGHT_OFF": Actions.flashlight(this, false); speak(reply); break;
            case "HOME": Actions.home(this); break;
            case "BACK": case "SCROLL_DOWN": case "SCROLL_UP": case "READ_SCREEN":
                String out = ControlService.perform(action);
                speak(out != null ? out : (reply == null ? "Done." : reply));
                break;
            default: speak(reply);
        }
    }

    private void openApp(String name, String reply) {
        String pkg = Actions.findPackage(this, name);
        if (pkg != null) { Actions.launch(this, pkg); speak(reply); }
        else speak("I could not find " + name + " on your phone.");
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }

    @Override public void onDestroy() {
        super.onDestroy();
        try { if (orb != null) wm.removeView(orb); } catch (Exception ignored) {}
        try { if (caption != null) wm.removeView(caption); } catch (Exception ignored) {}
        if (recognizer != null) recognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
    }
                                                      }
