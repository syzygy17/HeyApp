package com.github.portfolio.heyapp.Activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.portfolio.heyapp.Adapters.MessageAdapter;
import com.github.portfolio.heyapp.POJOs.Messages;
import com.github.portfolio.heyapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

/*
    The activity in which all the main functions related
    to the interaction between the interlocutors take place.

    Активность, в которой происходят все основные функции,
    связанные с взаимодействием между собеседниками.
*/

public class ChatActivity extends AppCompatActivity {


    private String messageReceiverID;
    private String messageSenderID;
    private String saveCurrentTime;
    private String saveCurrentDate;
    private String checker="";
    private String myURL="";
    private final List<Messages> messagesList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private final int REQUEST_CODE = 438;


    // Declare widgets
    private TextView username, userLastSeen;
    private CircleImageView userImage;
    private ImageButton sendMessageButton, sendFileButton;
    private EditText messageInputText;
    private RecyclerView userMessagesList;
    private Uri fileUri;
    private ProgressDialog loadingBar;


    // Firebase
    private DatabaseReference rootReference;
    private StorageTask uploadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        messageReceiverID = getIntent().getExtras().getString("visitorChatUserID");
        String messageReceiverName = getIntent().getExtras().getString("visitorChatUsername");
        String messageReceiverImage = getIntent().getExtras().getString("visitorChatUserImage");
        initializeFields();
        username.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage)
                .placeholder(R.drawable.profile_image).into(userImage);
        sendMessageButton.setOnClickListener(v -> sendMessage());
        displayLastSeen();
        sendFileButton.setOnClickListener(v -> {
            CharSequence[] options = new CharSequence[] {
                    getString(R.string.image_file_type),
                    getString(R.string.pdf_file_type),
                    getString(R.string.ms_word_file_type)
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
            builder.setTitle(getString(R.string.select_file_type));
            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        checker = "image";
                        Intent imageIntent = new Intent();
                        imageIntent.setAction(Intent.ACTION_GET_CONTENT);
                        imageIntent.setType("image/*");
                        startActivityForResult(imageIntent.createChooser(imageIntent, getString(R.string.select_image)), REQUEST_CODE);
                        break;
                    case 1:
                        checker = "pdf";
                        Intent pdfIntent = new Intent();
                        pdfIntent.setAction(Intent.ACTION_GET_CONTENT);
                        pdfIntent.setType("application/pdf");
                        startActivityForResult(pdfIntent.createChooser(pdfIntent, getString(R.string.select_pdf)), REQUEST_CODE);
                        break;
                    case 2:
                        checker = "docx";
                        Intent docxIntent = new Intent();
                        docxIntent.setAction(Intent.ACTION_GET_CONTENT);
                        docxIntent.setType("application/msword");
                        startActivityForResult(docxIntent.createChooser(docxIntent, getString(R.string.select_ms_word)), REQUEST_CODE);
                        break;
                }
            });
            builder.show();
        });
    }


    private void initializeFields() {
        Toolbar mToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);
        username = findViewById(R.id.custom_profile_name);
        userLastSeen = findViewById(R.id.custom_user_last_seen);
        userImage = findViewById(R.id.custom_profile_image);
        sendMessageButton = findViewById(R.id.send_chat_message_button);
        messageInputText = findViewById(R.id.input_message);
        sendFileButton = findViewById(R.id.send_file_button);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList.setLayoutManager(new LinearLayoutManager(this));
        userMessagesList.setAdapter(messageAdapter);
        loadingBar = new ProgressDialog(this);

        // Firebase
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        messageSenderID = firebaseAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();
    }


    /*
        In this method, we add messages from the Firebase database.
    */
    @Override
    protected void onStart() {
        super.onStart();
        rootReference.child("Messages").child(messageSenderID).child(messageReceiverID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        Messages messages = snapshot.getValue(Messages.class);
                        messagesList.add(messages);
                        messageAdapter.notifyDataSetChanged();

                        // when a new message is added, we scroll to the last message.
                        userMessagesList.smoothScrollToPosition
                                (userMessagesList.getAdapter().getItemCount());
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        messageAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        updateUserStatus("online");
    }


    /*
        In this method, we add messages, especially documents and images, to the Firebase storage.
    */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            loadingBar.setTitle(getString(R.string.send_image));
            loadingBar.setMessage(getString(R.string.please_wait));
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.show();
            fileUri = data.getData();
            if (!checker.equals("image")) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                        .child("Document Files");
                final String messageSenderReference = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverReference = "Messages/" + messageReceiverID + "/" + messageSenderID;
                DatabaseReference userMessageKeyReference = rootReference.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyReference.getKey();
                StorageReference filePath = storageReference.child(messagePushID + "." + checker);
                uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return filePath.getDownloadUrl();
                }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                    if (task.isSuccessful()) {
                        Uri downloadURL = task.getResult();
                        myURL = downloadURL.toString();

                        // Getting the current date and time for a message.
                        Calendar calendar = Calendar.getInstance();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate =
                                new SimpleDateFormat("dd MMMM, yyyy");
                        saveCurrentDate = currentDate.format(calendar.getTime());
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime =
                                new SimpleDateFormat("HH:mm");
                        saveCurrentTime = currentTime.format(calendar.getTime());

                        // Entering on the map all the necessary data about the message.
                        Map messageDocumentBody = new HashMap<>();
                        messageDocumentBody.put("message", myURL);
                        messageDocumentBody.put("name", fileUri.getLastPathSegment());
                        messageDocumentBody.put("type", checker);
                        messageDocumentBody.put("from", messageSenderID);
                        messageDocumentBody.put("to", messageReceiverID);
                        messageDocumentBody.put("messageID", messagePushID);
                        messageDocumentBody.put("time", saveCurrentTime);
                        messageDocumentBody.put("date", saveCurrentDate);
                        Map messageBodyDetails = new HashMap<>();
                        messageBodyDetails.put(messageSenderReference + "/" + messagePushID, messageDocumentBody);
                        messageBodyDetails.put(messageReceiverReference + "/" + messagePushID, messageDocumentBody);
                        rootReference.updateChildren(messageBodyDetails);
                        loadingBar.dismiss();
                        Toast.makeText(this,
                                getString(R.string.file_sent), Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    loadingBar.dismiss();
                    Toast.makeText(ChatActivity.this,
                            e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } else if (checker.equals("image")) {
                StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                        .child("Image Files");
                final String messageSenderReference = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverReference = "Messages/" + messageReceiverID + "/" + messageSenderID;
                DatabaseReference userMessageKeyReference = rootReference.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();
                final String messagePushID = userMessageKeyReference.getKey();
                StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");
                uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return filePath.getDownloadUrl();
                }).addOnCompleteListener((OnCompleteListener<Uri>) task -> {
                    if (task.isSuccessful()) {
                        Uri downloadURL = task.getResult();
                        myURL = downloadURL.toString();

                        // Getting the current date and time for a message.
                        Calendar calendar = Calendar.getInstance();
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate =
                                new SimpleDateFormat("dd MMMM, yyyy");
                        saveCurrentDate = currentDate.format(calendar.getTime());
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime =
                                new SimpleDateFormat("HH:mm");
                        saveCurrentTime = currentTime.format(calendar.getTime());

                        // Entering on the map all the necessary data about the message.
                        Map messageImageBody = new HashMap();
                        messageImageBody.put("message", myURL);
                        messageImageBody.put("name", fileUri.getLastPathSegment());
                        messageImageBody.put("type", checker);
                        messageImageBody.put("from", messageSenderID);
                        messageImageBody.put("to", messageReceiverID);
                        messageImageBody.put("messageID", messagePushID);
                        messageImageBody.put("time", saveCurrentTime);
                        messageImageBody.put("date", saveCurrentDate);
                        Map messageBodyDetails = new HashMap();
                        messageBodyDetails.put(messageSenderReference + "/" + messagePushID, messageImageBody);
                        messageBodyDetails.put(messageReceiverReference + "/" + messagePushID, messageImageBody);
                        rootReference.updateChildren(messageBodyDetails)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(this,
                                                getString(R.string.file_sent), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(this, task1.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                    loadingBar.dismiss();
                                });
                    }
                });
            } else {
                loadingBar.dismiss();
                Toast.makeText(this,
                        getString(R.string.error_select_file), Toast.LENGTH_LONG).show();
            }
        }
    }


    /*
        When using this method, the correct display of the user's status occurs.
    */
    private void displayLastSeen() {
        rootReference.child("Users").child(messageReceiverID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.hasChild("userState")) {
                                String state = snapshot.child("userState")
                                        .child("state").getValue().toString();
                                String date = snapshot.child("userState")
                                        .child("date").getValue().toString();
                                String time = snapshot.child("userState")
                                        .child("time").getValue().toString();
                                if (state.equals("online")) {
                                    userLastSeen.setText(getString(R.string.state_online));
                                } else {
                                    userLastSeen.setText(getString(R.string.last_seen) + date + " " + time);
                                }
                            } else {
                                userLastSeen.setText(getString(R.string.state_offline));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    /*
        To avoid duplicating all messages, all messages are deleted before destroying the activity.
    */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        messagesList.clear();
        messageAdapter.notifyDataSetChanged();
    }


    /*
        To avoid duplicating all messages, all messages are deleted before pausing the activity.
    */
    @Override
    protected void onPause() {
        super.onPause();
        messagesList.clear();
        messageAdapter.notifyDataSetChanged();
        updateUserStatus("offline");
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateUserStatus("online");
    }

    /*
        Method for successful and correct sending of a message.
    */
    private void sendMessage() {
        String messageText = messageInputText.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this,
                    getString(R.string.first_enter_message), Toast.LENGTH_SHORT).show();
        } else {
            String messageSenderReference = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverReference = "Messages/" + messageReceiverID + "/" + messageSenderID;
            DatabaseReference userMessageKeyReference = rootReference.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();
            String messagePushID = userMessageKeyReference.getKey();

            // Get the current date and time for a message.
            Calendar calendar = Calendar.getInstance();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate =
                    new SimpleDateFormat("dd MMMM, yyyy");
            saveCurrentDate = currentDate.format(calendar.getTime());
            @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime =
                    new SimpleDateFormat("HH:mm");
            saveCurrentTime = currentTime.format(calendar.getTime());

            // Entering on the map all the necessary data about the message.
            Map messageTextBody = new HashMap();
            messageTextBody.put("message", messageText);
            messageTextBody.put("type", "text");
            messageTextBody.put("from", messageSenderID);
            messageTextBody.put("to", messageReceiverID);
            messageTextBody.put("messageID", messagePushID);
            messageTextBody.put("time", saveCurrentTime);
            messageTextBody.put("date", saveCurrentDate);
            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderReference + "/" + messagePushID, messageTextBody);
            messageBodyDetails.put(messageReceiverReference + "/" + messagePushID, messageTextBody);
            rootReference.updateChildren(messageBodyDetails);
        }
        messageInputText.setText("");
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
        rootReference.child("Users").child(messageSenderID)
                .child("userState").updateChildren(onlineStateMap);
    }
}

