package com.github.portfolio.heyapp.Activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.portfolio.heyapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private final String LOG_TAG = getClass().getSimpleName();

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;

    // Declare widgets
    private Button loginButton;
    private EditText userEmail, userPassword;
    private TextView needNewAccountLink, forgotPasswordLink;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeFields();


        needNewAccountLink.setOnClickListener(v -> sendUserToRegisterActivity());

        loginButton.setOnClickListener(v -> allowUserToLogin());

    }

    private void allowUserToLogin() {
        String email = userEmail.getText().toString();
        String password = userPassword.getText().toString();

        if(TextUtils.isEmpty(email) && TextUtils.isEmpty(password)) {
            Toast.makeText(this,
                    "Please enter email and password", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email", Toast.LENGTH_SHORT).show();
        } else {
            loadingBar.setTitle("Sign in");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(true);
            loadingBar.show();
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String currentUserID = firebaseAuth.getCurrentUser().getUid();
                            String deviceToken = FirebaseInstanceId.getInstance().getToken();
                            usersReference.child(currentUserID)
                                    .child("device_token").setValue(deviceToken)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                loadingBar.dismiss();
                                                sendUserToMainActivity();
                                            }
                                        }
                                    });
                        } else {
                            String exception = task.getException().toString();
                            Log.d(LOG_TAG, "Exception: " + exception);

                            Toast.makeText(LoginActivity.this,
                                    "An error has occurred", Toast.LENGTH_SHORT).show();
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
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        loadingBar = new ProgressDialog(this);
        firebaseAuth = FirebaseAuth.getInstance();
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
    }

    private void sendUserToMainActivity() {
        getIntent(LoginActivity.this, MainActivity.class);
    }

    private void sendUserToRegisterActivity() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        finish();
    }

    private void getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}