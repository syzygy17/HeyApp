package com.github.portfolio.heyapp.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.portfolio.heyapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

/*
    In this Activity the user registers.

    В этом Активности пользователь регистрируется.
*/

public class RegisterActivity extends AppCompatActivity {


    // Declare widgets
    private Button createAccountButton;
    private EditText userEmail, userPassword;
    private TextView alreadyHaveAccountLink;
    private ProgressDialog loadingBar;


    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initializeFields();
        alreadyHaveAccountLink.setOnClickListener(v -> sendUserToLoginActivity());
        createAccountButton.setOnClickListener(v -> createNewAccount());
    }


    private void createNewAccount() {
        String email = userEmail.getText().toString();
        String password = userPassword.getText().toString();
        if (TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            Toast.makeText(this,
                    getString(R.string.enter_email_password), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, getString(R.string.enter_password), Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle(getString(R.string.creating_new_account));
            loadingBar.setMessage(getString(R.string.please_wait));
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            String currentUserID = firebaseAuth.getCurrentUser().getUid();
                            databaseReference.child("Users").child(currentUserID)
                                    .setValue("");
                            databaseReference.child("Users").child(currentUserID)
                                    .child("device_token").setValue(deviceToken)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            sendUserToMainActivity();
                                            Toast.makeText(this,
                                                    getString(R.string.account_created),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this,
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                        loadingBar.dismiss();
                    });
        }
    }


    private void initializeFields() {
        createAccountButton = findViewById(R.id.register_button);
        userEmail = findViewById(R.id.register_email);
        userPassword = findViewById(R.id.register_password);
        alreadyHaveAccountLink = findViewById(R.id.already_have_account_link);
        loadingBar = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();
    }


    private void sendUserToLoginActivity() {
        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        finish();
    }


    private void sendUserToMainActivity() {
        getIntent(RegisterActivity.this, MainActivity.class);
    }


    private void getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}