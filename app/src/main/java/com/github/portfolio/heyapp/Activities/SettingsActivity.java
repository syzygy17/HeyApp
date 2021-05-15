package com.github.portfolio.heyapp.Activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.portfolio.heyapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

/*
    In this Activity, the user can view and update their own settings.

    В этой активности пользователь может посмотреть и обновлять собственные настройки.
*/

public class SettingsActivity extends AppCompatActivity {


    private static final int GALLERY_PICK = 1;
    private String currentUserID;


    // Declare widgets
    private Button updateSettingsButton;
    private EditText username, userStatus;
    private CircleImageView userProfileImage;
    private ProgressDialog loadingBar;
    private Toolbar mToolbar;


    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference rootReference;
    private StorageReference userProfileImagesReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initializeFields();

        // If the account is created now, we show our user the username EditText. If no, we don't.
        username.setVisibility(View.INVISIBLE);

        retrieveUserInfo();
        updateSettingsButton.setOnClickListener(v -> updateSettings());
        userProfileImage.setOnClickListener(v -> {
            Intent galleryIntent = new Intent();
            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            galleryIntent.setType("image/*");
            startActivityForResult(galleryIntent, GALLERY_PICK);
        });
    }


    private void initializeFields() {
        updateSettingsButton = findViewById(R.id.update_settings_button);
        username = findViewById(R.id.set_username);
        userStatus = findViewById(R.id.set_profile_status);
        userProfileImage = findViewById(R.id.set_profile_image);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();
        userProfileImagesReference = FirebaseStorage.getInstance()
                .getReference().child("Profile Images");
        loadingBar = new ProgressDialog(this);
        mToolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.profile_settings));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null) {

            // start picker to get image for cropping and then use the image in cropping activity.
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                loadingBar.setTitle(getString(R.string.updating_profile_picture));
                loadingBar.setMessage(getString(R.string.please_wait));
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();
                Uri resultUri = result.getUri();
                StorageReference filePath = userProfileImagesReference
                        .child(currentUserID + ".jpg");
                filePath.putFile(resultUri).addOnSuccessListener(taskSnapshot ->
                        filePath.getDownloadUrl().addOnSuccessListener(uri -> {

                            // Setting the url to download the image in our user info in the database
                            final String downloadedUrl = uri.toString();
                            rootReference.child("Users").child(currentUserID).child("image")
                                    .setValue(downloadedUrl).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this,
                                            getString(R.string.profile_photo_updated),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this,
                                            task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                                loadingBar.dismiss();
                            });
                        }));
            }
        }
    }


    private void updateSettings() {
        String setUsername = username.getText().toString().trim();
        String setUserStatus = userStatus.getText().toString().trim();
        if (TextUtils.isEmpty(setUsername) && TextUtils.isEmpty(setUserStatus)) {
            Toast.makeText(this,
                    getString(R.string.enter_name_status), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(setUsername)) {
            Toast.makeText(this, getString(R.string.enter_name), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(setUserStatus)) {
            Toast.makeText(this, getString(R.string.enter_status), Toast.LENGTH_SHORT).show();
        } else {
            HashMap<String, Object> profileHashMap = new HashMap<>();
            profileHashMap.put("uid", currentUserID);
            profileHashMap.put("name", setUsername);
            profileHashMap.put("status", setUserStatus);
            rootReference.child("Users").child(currentUserID).updateChildren(profileHashMap)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this,
                                    getString(R.string.profile_updated),
                                    Toast.LENGTH_SHORT).show();
                            sendUserToMainActivity();
                        } else {
                            Toast.makeText(this,
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }


    private void retrieveUserInfo() {
        rootReference.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChild("name")
                                && snapshot.hasChild("image")) {

                            // Retrieving data from Firebase
                            String retrieveUsername = snapshot.child("name").getValue().toString();
                            String retrieveStatus = snapshot.child("status").getValue().toString();
                            String retrieveProfileImage = snapshot.child("image")
                                    .getValue().toString();

                            // Setting retrieved data to EditText
                            username.setText(retrieveUsername);
                            userStatus.setText(retrieveStatus);

                            // Updating profile image with Picasso library
                            Picasso.get().load(retrieveProfileImage)
                                    .placeholder(R.drawable.profile_image).into(userProfileImage);
                        } else if (snapshot.exists() && snapshot.hasChild("name")) {

                            // Retrieving data from Firebase
                            String retrieveUsername = snapshot.child("name").getValue().toString();
                            String retrieveStatus = snapshot.child("status").getValue().toString();

                            // Setting retrieved data to EditText
                            username.setText(retrieveUsername);
                            userStatus.setText(retrieveStatus);
                        } else {

                            // If the account is created now, we show our user the username EditText.
                            username.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void sendUserToMainActivity() {
        getIntent(SettingsActivity.this, MainActivity.class);
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