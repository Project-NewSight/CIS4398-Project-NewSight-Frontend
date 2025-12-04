package com.example.newsight.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RewardsHelper {
    private static final String PREF_NAME = "RewardsPrefs";
    private static final String KEY_POINTS = "points";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_LAST_LOGIN_DATE = "last_login_date";
    private static final String KEY_STREAK_DAYS = "streak_days";
    private static final int POINTS_PER_LEVEL = 2000;

    private SharedPreferences prefs;
    private Context context;

    public RewardsHelper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void addPoints(int points) {
        int currentPoints = getPoints();
        int newPoints = currentPoints + points;
        
        int currentLevel = getLevel();
        int newLevel = newPoints / POINTS_PER_LEVEL;

        if (newLevel > currentLevel) {
            // Level up!
            Toast.makeText(context, "Level Up! You reached Level " + newLevel, Toast.LENGTH_LONG).show();
            prefs.edit().putInt(KEY_LEVEL, newLevel).apply();
        }

        prefs.edit().putInt(KEY_POINTS, newPoints).apply();
        
        // Show toast for points added
        Toast.makeText(context, "+" + points + " Points!", Toast.LENGTH_SHORT).show();
    }

    public int getPoints() {
        return prefs.getInt(KEY_POINTS, 0);
    }

    public int getLevel() {
        return prefs.getInt(KEY_LEVEL, 0);
    }

    public int getProgress() {
        return getPoints() % POINTS_PER_LEVEL;
    }
    
    public int getStreakDays() {
        return prefs.getInt(KEY_STREAK_DAYS, 0);
    }

    public void reset() {
        prefs.edit().clear().apply();
    }

    public void checkDailyLogin() {
        String lastLoginDate = prefs.getString(KEY_LAST_LOGIN_DATE, "");
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (!currentDate.equals(lastLoginDate)) {
            // New day
            addPoints(10); // Daily check-in points
            
            // Check streak logic (simplified)
            // If last login was yesterday, increment streak. Else reset to 1.
            // For now, let's just increment it if it's a new day to simulate progress for the user
            int currentStreak = getStreakDays();
            prefs.edit().putInt(KEY_STREAK_DAYS, currentStreak + 1).apply();
            
            if ((currentStreak + 1) % 5 == 0) {
                 addPoints(100); // 5-day streak bonus
                 Toast.makeText(context, "5-Day Streak! +100 Points", Toast.LENGTH_LONG).show();
            }

            prefs.edit().putString(KEY_LAST_LOGIN_DATE, currentDate).apply();
        }
    }
}
