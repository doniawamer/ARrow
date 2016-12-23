package com.example.olivi.htn_ar_ui;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void navigateClicked(View v){
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.wikitude.samples");
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }

        //startActivity(new Intent(this, Example.class));
    }

    public void augmentVisionClicked(View v){
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.AR_HTN.AR_app");
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }

        //startActivity(new Intent(this, Example.class));
    }

}
