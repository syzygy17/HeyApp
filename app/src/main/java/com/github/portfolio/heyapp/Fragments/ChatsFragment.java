package com.github.portfolio.heyapp.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.github.portfolio.heyapp.Activities.ChatActivity;
import com.github.portfolio.heyapp.POJOs.Contacts;
import com.github.portfolio.heyapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


public class ChatsFragment extends Fragment {

    private String currentUserID;

    // Declare widgets
    private View privateChatsView;
    private RecyclerView chatsList;

    // Firebase
    private DatabaseReference chatsReference, usersReference;
    private FirebaseAuth firebaseAuth;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        privateChatsView = inflater.inflate(R.layout.fragment_chats, container, false);

        initializeFields();

        return privateChatsView;
    }

    private void initializeFields() {
        chatsList = privateChatsView.findViewById(R.id.chats_list);
        chatsList.setLayoutManager(new LinearLayoutManager(getContext()));
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        chatsReference = FirebaseDatabase.getInstance().getReference()
                .child("Contacts").child(currentUserID);
        usersReference = FirebaseDatabase.getInstance().getReference()
                .child("Users");
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(chatsReference, Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ChatsViewHolder holder, int position, @NonNull Contacts model) {
                        final String userID = getRef(position).getKey();
                        final String[] userImage = {"default_image"};
                        usersReference.child(userID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String profileName = snapshot.child("name").getValue().toString();

                                    holder.username.setText(profileName);

                                    if (snapshot.hasChild("userState")) {
                                        String state = snapshot.child("userState")
                                                .child("state").getValue().toString();
                                        String date = snapshot.child("userState")
                                                .child("date").getValue().toString();
                                        String time = snapshot.child("userState")
                                                .child("time").getValue().toString();
                                        if (state.equals("online")) {
                                            holder.userStatus.setText(getString(R.string.state_online));
                                        } else {
                                            holder.userStatus.setText(
                                                    getString(R.string.last_seen) +
                                                            date + " " + time);
                                        }
                                    } else {
                                        holder.userStatus.setText(getString(R.string.state_offline));
                                    }


                                    if (snapshot.hasChild("image")) {
                                        userImage[0] = snapshot.child("image").getValue().toString();
                                        Picasso.get().load(userImage[0]).placeholder(R.drawable.profile_image)
                                                .into(holder.profileImage);
                                    } else {
                                        holder.profileImage.setImageResource(R.drawable.profile_image);
                                    }

                                    holder.itemView.setOnClickListener
                                            (v -> sendUserToChatActivity(userID, profileName, userImage[0]));
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.user_item_layout,
                                        parent, false);
                        ChatsViewHolder viewHolder = new ChatsViewHolder(view);
                        return viewHolder;
                    }
                };
        chatsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {
        TextView username, userStatus;
        CircleImageView profileImage;
        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.user_profile_image);
        }
    }

    private void sendUserToChatActivity(String userID, String username, String userImage) {
        Intent intent = new Intent(getContext(), ChatActivity.class);
        intent.putExtra("visitorChatUserID", userID);
        intent.putExtra("visitorChatUsername", username);
        intent.putExtra("visitorChatUserImage", userImage);
        startActivity(intent);
    }
}


