package com.github.portfolio.heyapp.Activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.portfolio.heyapp.Notifications.APIService;
import com.github.portfolio.heyapp.Notifications.Client;
import com.github.portfolio.heyapp.Notifications.Data;
import com.github.portfolio.heyapp.Notifications.MyResponse;
import com.github.portfolio.heyapp.Notifications.NotificationSender;
import com.github.portfolio.heyapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/*
    In this Activity, we show the profile of any user.

    В этой Активности мы показываем профиль любого пользователя.
*/

public class ProfileActivity extends AppCompatActivity {


    private String receiverUserID, senderUserID, currentState, senderUsername;


    // Declare widgets
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestButton, declineMessageRequestButton;


    // Firebase
    private DatabaseReference usersReference, chatRequestsReference, contactsReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        initializeFields();
        retrieveUserInfo();
        usersReference.child(senderUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    senderUsername = snapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        updateUserStatus("online");
    }


    private void retrieveUserInfo() {
        usersReference.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChild("image")) {
                    String userImage = snapshot.child("image").getValue().toString();
                    String username = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();
                    Picasso.get().load(userImage)
                            .placeholder(R.drawable.profile_image)
                            .into(userProfileImage);
                    userProfileName.setText(username);
                    userProfileStatus.setText(userStatus);
                } else {
                    String username = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();

                    userProfileName.setText(username);
                    userProfileStatus.setText(userStatus);
                }
                manageChatRequests();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void manageChatRequests() {
        chatRequestsReference.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(receiverUserID)) {
                            String requestType = snapshot.child(receiverUserID)
                                    .child("request_type").getValue().toString();
                            if (requestType.equals("sent")) {
                                currentState = "request_sent";
                                sendMessageRequestButton.setText(getString(R.string.cancel_chat_request));
                            } else if (requestType.equals("received")) {
                                currentState = "request_received";
                                sendMessageRequestButton.setText(getString(R.string.accept_chat_request));
                                declineMessageRequestButton.setVisibility(View.VISIBLE);
                                declineMessageRequestButton.setEnabled(true);
                                declineMessageRequestButton
                                        .setOnClickListener(v -> cancelChatRequest());
                            }
                        } else {
                            contactsReference.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.hasChild(receiverUserID)) {
                                                currentState = "friends";
                                                sendMessageRequestButton.setText(getString
                                                        (R.string.delete_contact));
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        if (!senderUserID.equals(receiverUserID)) {
            sendMessageRequestButton.setOnClickListener(v -> {
                sendMessageRequestButton.setEnabled(false);
                if (currentState.equals("new")) {
                    sendChatRequest();
                }
                if (currentState.equals("request_sent")) {
                    cancelChatRequest();
                }
                if (currentState.equals("request_received")) {
                    acceptChatRequest();
                }
                if (currentState.equals("friends")) {
                    removeSpecificContact();
                }
            });
        } else {
            sendMessageRequestButton.setVisibility(View.INVISIBLE);
        }
    }


    private void removeSpecificContact() {
        contactsReference.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contactsReference.child(receiverUserID).child(senderUserID)
                                .removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        sendMessageRequestButton.setEnabled(true);
                                        currentState = "new";
                                        sendMessageRequestButton.setText(R.string.send_message);
                                        declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                        declineMessageRequestButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }


    private void acceptChatRequest() {
        contactsReference.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contactsReference.child(receiverUserID).child(senderUserID)
                                .child("Contacts").setValue("Saved")
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        chatRequestsReference.child(senderUserID)
                                                .child(receiverUserID)
                                                .removeValue()
                                                .addOnCompleteListener(task11 -> {
                                                    if (task11.isSuccessful()) {
                                                        chatRequestsReference.child(receiverUserID)
                                                                .child(senderUserID)
                                                                .removeValue()
                                                                .addOnCompleteListener(task111 -> {
                                                                    if (task111.isSuccessful()) {
                                                                        sendMessageRequestButton.setEnabled(true);
                                                                        currentState = "friends";
                                                                        sendMessageRequestButton.setText(getString(R.string.delete_contact));
                                                                        declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                                        declineMessageRequestButton.setEnabled(false);
                                                                    }
                                                                });
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }


    private void cancelChatRequest() {
        chatRequestsReference.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestsReference.child(receiverUserID).child(senderUserID)
                                .removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        sendMessageRequestButton.setEnabled(true);
                                        currentState = "new";
                                        sendMessageRequestButton.setText(R.string.send_message);
                                        declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                        declineMessageRequestButton.setEnabled(false);
                                    }
                                });
                    }
                });
    }


    private void sendChatRequest() {
        chatRequestsReference.child(senderUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestsReference.child(receiverUserID).child(senderUserID)
                                .child("request_type").setValue("received")
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        usersReference.child(receiverUserID).
                                                addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                        // send notification about new chat request for receiver device.
                                                        if (snapshot.hasChild("device_token")) {
                                                            String receiverToken = snapshot.child("device_token").getValue().toString();
                                                            APIService apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);
                                                            sendNotification(receiverToken, getString(R.string.friend_request),
                                                                    senderUsername + getString(R.string.sent_friend_request), apiService);
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                        sendMessageRequestButton.setEnabled(true);
                                        currentState = "request_sent";
                                        sendMessageRequestButton.setText(getString(R.string.cancel_chat_request));
                                    }
                                });
                    }
                });
    }


    /*
        Method of sending a notification about a new request to the chat.
    */
    private void sendNotification(String userToken, String title, String message, APIService apiService) {
        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(data, userToken);
        apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    if (response.body().success != 1) {
                        Toast.makeText(ProfileActivity.this,
                                getString(R.string.no_notification_sent), Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {

            }
        });
    }


    private void initializeFields() {
        userProfileName = findViewById(R.id.visitor_username);
        userProfileStatus = findViewById(R.id.visitor_profile_status);
        userProfileImage = findViewById(R.id.visitor_profile_image);
        sendMessageRequestButton = findViewById(R.id.send_message_request_button);
        declineMessageRequestButton = findViewById(R.id.decline_message_request_button);
        receiverUserID = getIntent().getExtras().get("visitorUserID").toString();
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestsReference = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        contactsReference = FirebaseDatabase.getInstance().getReference().child("Contacts");
        currentState = "new";
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        senderUserID = firebaseAuth.getCurrentUser().getUid();
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
        usersReference.child(senderUserID)
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