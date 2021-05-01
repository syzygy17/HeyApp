package com.github.portfolio.heyapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.portfolio.heyapp.Notifications.APIService;
import com.github.portfolio.heyapp.Notifications.Client;
import com.github.portfolio.heyapp.Notifications.Data;
import com.github.portfolio.heyapp.Notifications.MyResponse;
import com.github.portfolio.heyapp.Notifications.NotificationSender;
import com.github.portfolio.heyapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID, senderUserID, currentState;


    // Declare widgets
    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button sendMessageRequestButton, declineMessageRequestButton;

    // Firebase
    private DatabaseReference usersReference, chatRequestsReference, contactsReference;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initializeFields();

        retrieveUserInfo();

        //
        updateUserStatus("online");
        //
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
                                sendMessageRequestButton.setText("Сancel chat request");
                            } else if (requestType.equals("received")){
                                currentState = "request_received";
                                sendMessageRequestButton.setText("Accept chat request");
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
                                                sendMessageRequestButton.setText("Delete contact");
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
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            contactsReference.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                sendMessageRequestButton.setEnabled(true);
                                                currentState = "new";
                                                sendMessageRequestButton.setText(R.string.send_message);

                                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                declineMessageRequestButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void acceptChatRequest() {
        contactsReference.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            contactsReference.child(receiverUserID).child(senderUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                chatRequestsReference.child(senderUserID)
                                                        .child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    chatRequestsReference.child(receiverUserID)
                                                                            .child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        sendMessageRequestButton.setEnabled(true);
                                                                                        currentState = "friends";
                                                                                        sendMessageRequestButton.setText("Delete contact");

                                                                                        declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                                                        declineMessageRequestButton.setEnabled(false);
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void cancelChatRequest() {
        chatRequestsReference.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            chatRequestsReference.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                sendMessageRequestButton.setEnabled(true);
                                                currentState = "new";
                                                sendMessageRequestButton.setText(R.string.send_message);

                                                declineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                declineMessageRequestButton.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void sendChatRequest() {
        chatRequestsReference.child(senderUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            chatRequestsReference.child(receiverUserID).child(senderUserID)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                //
                                                // TODO Вроде работает отправка уведомления о чат запросе, но постараться понять и отрефакторить это
                                                // TODO а также рассмотреть несколько вариантов в Trello -
                                                //  https://trello.com/c/GT2ykMsW/71-%D0%BF%D0%BE%D0%B4%D1%83%D0%BC%D0%B0%D1%82%D1%8C-%D0%BE-%D1%82%D0%BE%D0%BC-%D1%87%D1%82%D0%BE-%D0%B4%D0%B5%D0%BB%D0%B0%D1%82%D1%8C
                                                /*usersReference.child(receiverUserID).
                                                        addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                if (snapshot.hasChild("device_token")) {
                                                                    String receiverToken = snapshot.child("device_token").getValue().toString();
                                                                    APIService apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);
                                                                    sendNotification(receiverToken, "Chat request", "A new chat request, please check", apiService);
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });*/
                                                //
                                                sendMessageRequestButton.setEnabled(true);
                                                currentState = "request_sent";
                                                sendMessageRequestButton.setText("Сancel chat request");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void sendNotification(String userToken, String title, String message, APIService apiService) {
        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(data, userToken);
        apiService.sendNotification(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    if (response.body().success != 1) {
                        Toast.makeText(ProfileActivity.this, "Failed", Toast.LENGTH_LONG).show();
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
        firebaseAuth = FirebaseAuth.getInstance();
        senderUserID = firebaseAuth.getCurrentUser().getUid();
    }

    //
    private void updateUserStatus(String state) {
        String saveCurrentTime, saveCurrentDate;

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("dd MMMM, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", saveCurrentTime);
        onlineStateMap.put("date", saveCurrentDate);
        onlineStateMap.put("state", state);

        usersReference.child(senderUserID)
                .child("userState").updateChildren(onlineStateMap);
    }
    //


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //
        updateUserStatus("online");
        //
    }

    @Override
    protected void onPause() {
        super.onPause();
        //
        updateUserStatus("offline");
        //
    }

    @Override
    protected void onStop() {
        super.onStop();
        //
        updateUserStatus("offline");
        //
    }

    @Override
    protected void onResume() {
        super.onResume();
        //
        updateUserStatus("online");
        //
    }

    @Override
    protected void onStart() {
        super.onStart();
        //
        updateUserStatus("online");
        //
    }
}