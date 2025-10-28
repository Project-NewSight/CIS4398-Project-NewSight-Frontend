package com.example.newsight;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;



import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.*;


public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Toast.makeText(this, "Splash loaded", Toast.LENGTH_SHORT).show();
        /*new Handler().postDelayed(new Runnable(){
            @Override
            public void run(){
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }3000);*/
        Thread thread = new Thread(){
            @Override
            public void run() {

                try{
                    sleep(3000);
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    startActivity(new Intent(splash.this,MainActivity.class));
                }
            }
        };
        thread.start();
    }
}