package com.github.portfolio.heyapp.Activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.portfolio.heyapp.Adapters.MessageAdapter;
import com.github.portfolio.heyapp.POJOs.Messages;
import com.github.portfolio.heyapp.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverID, messageReceiverName, messageReceiverImage, messageSenderID,
            saveCurrentTime, saveCurrentDate, checker="", myURL="";
    private final List<Messages> messagesList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private final int REQUEST_CODE = 438;


    // Declare widgets
    private TextView username, userLastSeen;
    private CircleImageView userImage;
    private Toolbar mToolbar;
    private ImageButton sendMessageButton, sendFileButton;
    private EditText messageInputText;
    private LinearLayoutManager linearLayoutManager;
    private RecyclerView userMessagesList;
    private Uri fileUri;
    private ProgressDialog loadingBar;


    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference rootReference;
    private StorageTask uploadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        messageReceiverID = getIntent().getExtras().getString("visitorChatUserID");
        messageReceiverName = getIntent().getExtras().getString("visitorChatUsername");
        messageReceiverImage = getIntent().getExtras().getString("visitorChatUserImage");

        initializeFields();
        username.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage)
                .placeholder(R.drawable.profile_image).into(userImage);

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        displayLastSeen();

        sendFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CharSequence[] options = new CharSequence[] {
                        "Images",
                        "PDF files",
                        "MS Word files"
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select file type");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            checker = "image";
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent, "Select image"), REQUEST_CODE);
                        } else if (which == 1) {
                            checker = "pdf";
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent, "Select PDF file"), REQUEST_CODE);
                        } else if (which == 2) {
                            checker = "docx";
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent, "Select MS Word file"), REQUEST_CODE);
                        }
                    }
                });
                builder.show();
            }
        });
    }

    private void initializeFields() {

        mToolbar = findViewById(R.id.chat_toolbar);
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

        firebaseAuth = FirebaseAuth.getInstance();
        messageSenderID = firebaseAuth.getCurrentUser().getUid();
        rootReference = FirebaseDatabase.getInstance().getReference();

        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = findViewById(R.id.private_messages_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        loadingBar = new ProgressDialog(this);
    }

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
        //
        updateUserStatus("online");
        //
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null
                && data.getData() != null) {

            loadingBar.setTitle("Sending the image");
            loadingBar.setMessage("Please wait");
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
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadURL = task.getResult();
                            myURL = downloadURL.toString();
                            // Getting current date and time for the message
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat currentDate = new SimpleDateFormat("dd MMMM, yyyy");
                            saveCurrentDate = currentDate.format(calendar.getTime());
                            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
                            saveCurrentTime = currentTime.format(calendar.getTime());


                            Map messageDocumentBody = new HashMap();
                            messageDocumentBody.put("message", myURL);
                            messageDocumentBody.put("name", fileUri.getLastPathSegment());
                            messageDocumentBody.put("type", checker);
                            messageDocumentBody.put("from", messageSenderID);
                            messageDocumentBody.put("to", messageReceiverID);
                            messageDocumentBody.put("messageID", messagePushID);
                            messageDocumentBody.put("time", saveCurrentTime);
                            messageDocumentBody.put("date", saveCurrentDate);

                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderReference + "/" + messagePushID, messageDocumentBody);
                            messageBodyDetails.put(messageReceiverReference + "/" + messagePushID, messageDocumentBody);

                            rootReference.updateChildren(messageBodyDetails);
                            loadingBar.dismiss();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this,
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (checker.equals("image")){
                StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                        .child("Image Files");

                final String messageSenderReference = "Messages/" + messageSenderID + "/" + messageReceiverID;
                final String messageReceiverReference = "Messages/" + messageReceiverID + "/" + messageSenderID;

                DatabaseReference userMessageKeyReference = rootReference.child("Messages")
                        .child(messageSenderID).child(messageReceiverID).push();

                final String messagePushID = userMessageKeyReference.getKey();

                StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");
                uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {
                            Uri downloadURL = task.getResult();
                            myURL = downloadURL.toString();

                            // Getting current date and time for the message
                            Calendar calendar = Calendar.getInstance();
                            SimpleDateFormat currentDate = new SimpleDateFormat("dd MMMM, yyyy");
                            saveCurrentDate = currentDate.format(calendar.getTime());
                            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
                            saveCurrentTime = currentTime.format(calendar.getTime());

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
                                    .addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {
                                            if (task.isSuccessful()) {
                                                loadingBar.dismiss();
                                                Toast.makeText(ChatActivity.this,
                                                        "Image message sent", Toast.LENGTH_SHORT).show();
                                            } else {
                                                loadingBar.dismiss();
                                                Toast.makeText(ChatActivity.this,
                                                        "Error", Toast.LENGTH_SHORT).show();
                                            }
                                            // messageInputText.setText("");
                                        }
                                    });
                        }
                    }
                });
            } else {
                loadingBar.dismiss();
                Toast.makeText(this,
                        "An error occurred, please select an image", Toast.LENGTH_SHORT).show();
            }
        }
    }

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
                                    userLastSeen.setText("online");
                                } else {
                                    userLastSeen.setText("Last seen: " + date + " " + time);
                                }
                            } else {
                                userLastSeen.setText("offline");
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        messagesList.clear();
        messageAdapter.notifyDataSetChanged();
        //
        updateUserStatus("online");
        //
    }

    @Override
    protected void onPause() {
        super.onPause();
        messagesList.clear();
        messageAdapter.notifyDataSetChanged();
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

    private void sendMessage() {
        String messageText = messageInputText.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this,
                    "First enter your message", Toast.LENGTH_SHORT).show();
            messageInputText.setText("");
        } else {
            String messageSenderReference = "Messages/" + messageSenderID + "/" + messageReceiverID;
            String messageReceiverReference = "Messages/" + messageReceiverID + "/" + messageSenderID;

            DatabaseReference userMessageKeyReference = rootReference.child("Messages")
                    .child(messageSenderID).child(messageReceiverID).push();

            String messagePushID = userMessageKeyReference.getKey();

            // Getting current date and time for the message
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd MMMM, yyyy");
            saveCurrentDate = currentDate.format(calendar.getTime());
            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
            saveCurrentTime = currentTime.format(calendar.getTime());


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

            rootReference.updateChildren(messageBodyDetails)
                    .addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(ChatActivity.this,
                                        "Message sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ChatActivity.this,
                                        "Error", Toast.LENGTH_SHORT).show();
                            }
                            messageInputText.setText("");
                        }
                    });
        }
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

        rootReference.child("Users").child(messageSenderID)
                .child("userState").updateChildren(onlineStateMap);
    }
    //
}

