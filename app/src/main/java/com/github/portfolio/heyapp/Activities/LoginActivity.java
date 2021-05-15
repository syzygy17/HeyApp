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
    In this activity, we will authorize the user.

    В этой Активности мы авторизуем пользователя.
*/

public class LoginActivity extends AppCompatActivity {


    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;


    // Declare widgets
    private Button loginButton;
    private EditText userEmail, userPassword;
    private TextView needNewAccountLink;
    private ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initializeFields();
        needNewAccountLink.setOnClickListener(v -> sendUserToRegisterActivity());
        loginButton.setOnClickListener(v -> allowUserToLogin());
    }


    /*
        After clicking on the login button and entering the correct account data,
        the user is taken to the main screen.
    */
    private void allowUserToLogin() {
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
            loadingBar.setTitle(getString(R.string.sign_in));
            loadingBar.setMessage(getString(R.string.please_wait));
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String currentUserID = firebaseAuth.getCurrentUser().getUid();
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            usersReference.child(currentUserID)
                                    .child("device_token").setValue(deviceToken)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            loadingBar.dismiss();
                                            sendUserToMainActivity();
                                        }
                                    });
                        } else {
                            Toast.makeText(this,
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            loadingBar.dismiss();
                        }
                    });
        }
    }


    private void initializeFields() {
        loginButton = findViewById(R.id.login_button);
        userEmail = findViewById(R.id.login_email);
        userPassword = findViewById(R.id.login_password);
        needNewAccountLink = findViewById(R.id.need_new_account_link);
        loadingBar = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
    }


    private void sendUserToMainActivity() {
        getIntent(LoginActivity.this, MainActivity.class);
    }


    private void sendUserToRegisterActivity() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }


    private void getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}