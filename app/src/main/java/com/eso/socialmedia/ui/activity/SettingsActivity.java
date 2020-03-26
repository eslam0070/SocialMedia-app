package com.eso.socialmedia.ui.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.eso.socialmedia.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat mPostSwitch;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private static final String TOPIC_POST_NOTIFICATION = "POST";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar_post_liked);
        toolbar.setTitle("Settings");
        sp = getSharedPreferences("Notification_SP",0);

        mPostSwitch = findViewById(R.id.postSwitch);
        boolean isPostEnabled = sp.getBoolean(""+TOPIC_POST_NOTIFICATION,false);
        if (isPostEnabled)
            mPostSwitch.setChecked(true);
        else
            mPostSwitch.setChecked(false);
        mPostSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor = sp.edit();
            editor.putBoolean(""+TOPIC_POST_NOTIFICATION,isChecked);
            editor.apply();
            if (isChecked)
                subscribePostNotification();
            else {
                unSubscribePostNotification();
            }
        });
    }

    private void unSubscribePostNotification() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(""+TOPIC_POST_NOTIFICATION).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                String msg = "You will not receive post notification";
                if (!task.isSuccessful())
                    msg = "UnSubscription failed";
                Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void subscribePostNotification() {
        FirebaseMessaging.getInstance().subscribeToTopic(""+TOPIC_POST_NOTIFICATION).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                String msg = "You will receive post notification";
                if (!task.isSuccessful())
                    msg = "Subscription Successfully";
                Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
