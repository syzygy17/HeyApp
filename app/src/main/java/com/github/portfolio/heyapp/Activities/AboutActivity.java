package com.github.portfolio.heyapp.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.portfolio.heyapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/*
    A activity that provides detailed information about an application and its author.

    Класс, предоставляющий подробную информацию о приложении и его авторе.
*/

public class AboutActivity extends AppCompatActivity {


    private String currentUserID;
    private DatabaseReference rootReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        initializeFields();
        updateUserStatus("online");
    }


    private void initializeFields() {
        Toolbar mToolbar = findViewById(R.id.about_toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
        }
        getSupportActionBar().setTitle(getString(R.string.about_heyapp_menu));
        // Add the Up button.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Firebase
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();
    }


    /*
        Due to the method updateUserStatus
        you can change the status of the user depending on the application state.
    */
    private void updateUserStatus(String state) {
        String saveCurrentTime, saveCurrentDate;
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate =
                new SimpleDateFormat("dd MMMM, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime =
                new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());
        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", saveCurrentTime);
        onlineStateMap.put("date", saveCurrentDate);
        onlineStateMap.put("state", state);
        rootReference.child("Users").child(currentUserID)
                .child("userState").updateChildren(onlineStateMap);
    }


    @Override
    protected void onPause() {
        super.onPause();
        updateUserStatus("offline");
    }


    @Override
    protected void onStop() {
        super.onStop();
        updateUserStatus("offline");
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateUserStatus("online");
    }


    @Override
    protected void onStart() {
        super.onStart();
        updateUserStatus("online");
    }
}