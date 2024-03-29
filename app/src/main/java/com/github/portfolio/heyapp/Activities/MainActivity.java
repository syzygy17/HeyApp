package com.github.portfolio.heyapp.Activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.github.portfolio.heyapp.Adapters.TabsAdapter;
import com.github.portfolio.heyapp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

/*
    This activity shows all the important information about chats,
    contacts and requests, as well as users can proceed to other actions.

    В этой активности показывается вся важная информация о чатах, контактах и запросах,
    а также пользователи могут переходить к другим действиям.
*/

public class MainActivity extends AppCompatActivity {


    private String currentUserID;


    // Declare widgets
    private Toolbar mToolbar;
    private ViewPager mViewPager;
    private TabLayout mTabLayout;


    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFields();

        // Setting custom toolbar with app's name.
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));

        // Setting tabs adapter on view pager and then we're setting view pager on tab layout.
        TabsAdapter mTabsAdapter = new TabsAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mTabsAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }


    private void initializeFields() {
        mToolbar = findViewById(R.id.main_page_toolbar);
        mViewPager = findViewById(R.id.main_tabs_pager);
        mTabLayout = findViewById(R.id.main_tabs);
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
        if (firebaseAuth.getCurrentUser() != null) {
            currentUserID = firebaseAuth.getCurrentUser().getUid();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            firebaseAuth.signOut();
            sendUserToLoginActivity();
        } else {
            updateUserStatus("online");
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            updateUserStatus("online");
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            updateUserStatus("offline");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.main_find_friends_option:
                sendUserToFindFriendsActivity();
                break;
            case R.id.main_settings_option:
                sendUserToSettingsActivity();
                break;
            case R.id.main_about_app_option:
                sendUserToAboutActivity();
                break;
            case R.id.main_log_out_option:
                updateUserStatus("offline");
                firebaseAuth.signOut();
                sendUserToLoginActivity();
                break;
        }
        return true;
    }


    private void sendUserToFindFriendsActivity() {
        startActivity(new Intent(MainActivity.this, FindFriendsActivity.class));
    }


    private void sendUserToLoginActivity() {
        getIntent(MainActivity.this, LoginActivity.class);
    }


    private void sendUserToSettingsActivity() {
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
    }


    private void sendUserToAboutActivity() {
        startActivity(new Intent(MainActivity.this, AboutActivity.class));
    }


    private void getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void updateUserStatus(String state) {
        String saveCurrentTime, saveCurrentDate;
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate =
                new SimpleDateFormat("dd MMMM, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());
        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());
        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", saveCurrentTime);
        onlineStateMap.put("date", saveCurrentDate);
        onlineStateMap.put("state", state);
        databaseReference.child("Users").child(currentUserID)
                .child("userState").updateChildren(onlineStateMap);
    }
}