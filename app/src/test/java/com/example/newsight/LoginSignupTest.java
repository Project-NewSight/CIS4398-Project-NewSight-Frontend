package com.example.newsight;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowToast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 33)
public class LoginSignupTest {

    @Before
    public void setup() {
        // Clear shared preferences
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    @Test
    public void testSignupFlow() {
        // Start SignupActivity
        SignupActivity signupActivity = Robolectric.buildActivity(SignupActivity.class)
                .create().start().resume().visible().get();

        // Get views
        EditText etName = signupActivity.findViewById(R.id.etName);
        EditText etEmail = signupActivity.findViewById(R.id.etEmail);
        EditText etPassword = signupActivity.findViewById(R.id.etPassword);
        EditText etConfirmPassword = signupActivity.findViewById(R.id.etConfirmPassword);
        Button btnSignup = signupActivity.findViewById(R.id.btnSignup);

        // Fill form
        etName.setText("Test User");
        etEmail.setText("test@example.com");
        etPassword.setText("password123");
        etConfirmPassword.setText("password123");

        // Click signup directly calling method or performing click
        btnSignup.performClick();

        // Verify data saved in SharedPreferences
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        assertEquals("test@example.com", prefs.getString("user_email", ""));
        assertEquals("password123", prefs.getString("user_password", ""));
        
        // Check if MainActivity was started
        ShadowActivity shadowActivity = Shadows.shadowOf(signupActivity);
        Intent nextIntent = shadowActivity.getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(MainActivity.class.getCanonicalName(), nextIntent.getComponent().getClassName());
    }

    @Test
    public void testLoginFlow() {
        // Pre-populate user in SharedPreferences
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("user_email", "user@test.com")
                .putString("user_password", "secret")
                .apply();

        // Start MainActivity
        MainActivity mainActivity = Robolectric.buildActivity(MainActivity.class)
                .create().start().resume().visible().get();

        // Get views
        EditText etEmail = mainActivity.findViewById(R.id.etEmail);
        EditText etPassword = mainActivity.findViewById(R.id.etPassword);
        Button btnLogin = mainActivity.findViewById(R.id.btnLogin);

        // Fill login form
        etEmail.setText("user@test.com");
        etPassword.setText("secret");

        // Click login
        btnLogin.performClick();
        
        // Check toast for success message
        String latestToast = ShadowToast.getTextOfLatestToast();
        assertTrue(latestToast.contains("Logged in"));

        // Check if LoadingActivity was started
        ShadowActivity shadowActivity = Shadows.shadowOf(mainActivity);
        Intent nextIntent = shadowActivity.getNextStartedActivity();
        assertNotNull(nextIntent);
        assertEquals(LoadingActivity.class.getCanonicalName(), nextIntent.getComponent().getClassName());
    }
}
