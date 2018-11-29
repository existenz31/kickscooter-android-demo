package com.vulog.kickscooter;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.vulog.kickscooter.ui.scooterinfo.ScooterInfoFragment;

public class ScooterInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scooter_info_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ScooterInfoFragment.newInstance())
                    .commitNow();
        }
    }
}
