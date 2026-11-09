package com.example.accidentreportandmonitoringsystem;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable vector drawables support for BottomNavigationView and image views on API 23+ devices
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Force Light Mode globally so ARMS design colors render correctly on all devices regardless of system dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // Global crash handler to catch unhandled exceptions, log them, and show toast instead of silent exit
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                try {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplicationContext(), "ARMS Error: " + e.getClass().getSimpleName() + " - " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                } catch (Exception ignored) {
                }
            }
        });
    }
}
