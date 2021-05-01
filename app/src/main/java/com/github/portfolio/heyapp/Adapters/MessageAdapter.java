package com.github.portfolio.heyapp.Adapters;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.portfolio.heyapp.Activities.ChatActivity;
import com.github.portfolio.heyapp.Activities.ImageViewerActivity;
import com.github.portfolio.heyapp.Activities.MainActivity;
import com.github.portfolio.heyapp.POJOs.Messages;
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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Messages> userMessagesList;

    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersReference;

    public MessageAdapter(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_messages_layout,
                        parent,
                        false);
        firebaseAuth = FirebaseAuth.getInstance();
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        String messageSenderID = firebaseAuth.getCurrentUser().getUid();
        Messages messages = userMessagesList.get(position);

        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        usersReference = FirebaseDatabase.getInstance().getReference().child("Users")
                .child(fromUserID);
        usersReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("image")) {
                    String receiverImage = snapshot.child("image").getValue().toString();
                    Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image)
                            .into(holder.receiverProfileImage);
                } else {
                    holder.receiverProfileImage.setImageResource(R.drawable.profile_image);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfileImage.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);

        if (fromMessageType.equals("text")) {
            if (fromUserID.equals(messageSenderID)) {
                holder.senderMessageText.setVisibility(View.VISIBLE);

                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setText(messages.getMessage() + "\n \n"
                        + messages.getTime() + " - " + messages.getDate());
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setText(messages.getMessage() + "\n \n"
                        + messages.getTime() + " - " + messages.getDate());
            }
        } else if (fromMessageType.equals("image")) {
            if (fromUserID.equals(messageSenderID)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPicture);
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPicture);
            }
        } else {
            if (fromUserID.equals(messageSenderID)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);

                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/heyapp-3c7ce.appspot.com/o/Image%20Files%2Ffile.png?alt=media&token=5fab2330-b392-48f1-8f55-0b7714d37d0f")
                        .into(holder.messageSenderPicture);
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);

                Picasso.get().load("https://firebasestorage.googleapis.com/v0/b/heyapp-3c7ce.appspot.com/o/Image%20Files%2Ffile.png?alt=media&token=5fab2330-b392-48f1-8f55-0b7714d37d0f")
                        .into(holder.messageReceiverPicture);
            }
        }

        if (fromUserID.equals(messageSenderID)) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userMessagesList.get(position).getType().equals("pdf") ||
                            userMessagesList.get(position).getType().equals("docx")) {
                        CharSequence[] options = new CharSequence[] {
                                "Delete for me",
                                "Download and view this document",
                                "Cancel",
                                "Delete for everyone"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteSentMessage(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 3) {
                                    deleteMessageForEveryone(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("text")) {
                        CharSequence[] options = new CharSequence[] {
                                "Delete for me",
                                "Cancel",
                                "Delete for everyone"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteSentMessage(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 2) {
                                    deleteMessageForEveryone(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("image")) {
                        CharSequence[] options = new CharSequence[] {
                                "Delete for me",
                                "View this image",
                                "Cancel",
                                "Delete for everyone"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteSentMessage(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    viewImage(position, holder);
                                } else if (which == 3) {
                                    deleteMessageForEveryone(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        } else {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (userMessagesList.get(position).getType().equals("pdf") ||
                            userMessagesList.get(position).getType().equals("docx")) {
                        CharSequence[] options = new CharSequence[] {
                                "Delete for me",
                                "Download and view this document",
                                "Cancel"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceivedMessage(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW,
                                            Uri.parse(userMessagesList.get(position).getMessage()));
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("text")) {
                        CharSequence[] options = new CharSequence[] {
                                "Delete for me",
                                "Cancel"
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceivedMessage(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    } else if (userMessagesList.get(position).getType().equals("image")) {
                        CharSequence[] options = new CharSequence[] {
                                "Delete for me",
                                "View this image",
                                "Cancel",
                        };
                        AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                        builder.setTitle("Delete message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    deleteReceivedMessage(position, holder);
                                    Intent intent = new Intent(holder.itemView.getContext(), MainActivity.class);
                                    holder.itemView.getContext().startActivity(intent);
                                } else if (which == 1) {
                                    viewImage(position, holder);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        }
    }

    private void deleteSentMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        rootReference.child("Messages")
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(holder.itemView.getContext(),
                            "The message was successfully deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(holder.itemView.getContext(),
                            "Error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteReceivedMessage(final int position, final MessageViewHolder holder) {
        DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        rootReference.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(holder.itemView.getContext(),
                            "The message was successfully deleted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(holder.itemView.getContext(),
                            "Error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteMessageForEveryone(final int position, final MessageViewHolder holder) {
        final DatabaseReference rootReference = FirebaseDatabase.getInstance().getReference();
        rootReference.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    rootReference.child("Messages")
                            .child(userMessagesList.get(position).getFrom())
                            .child(userMessagesList.get(position).getTo())
                            .child(userMessagesList.get(position).getMessageID())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(holder.itemView.getContext(),
                                        "The message was successfully deleted", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(holder.itemView.getContext(),
                                        "Error occurred", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    Toast.makeText(holder.itemView.getContext(),
                            "Error occurred", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void viewImage(final int position, final MessageViewHolder holder) {
        Intent intent = new Intent(holder.itemView.getContext(), ImageViewerActivity.class);
        intent.putExtra("url", userMessagesList.get(position).getMessage());
        holder.itemView.getContext().startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView senderMessageText, receiverMessageText;
        public CircleImageView receiverProfileImage;
        public ImageView messageSenderPicture, messageReceiverPicture;
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageText = itemView.findViewById(R.id.sender_message_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
        }
    }
}
