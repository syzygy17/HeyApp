package com.github.portfolio.heyapp.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
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


public class ContactsFragment extends Fragment {


    private String currentUserID;

    // Declare widgets
    private View contactsView;
    private RecyclerView myContactsList;

    // Firebase
    private DatabaseReference contactsReference, usersReference;
    private FirebaseAuth firebaseAuth;

    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        contactsView = inflater.inflate(R.layout.fragment_contacts, container, false);
        initializeFields();
        return contactsView;
    }

    private void initializeFields() {
        myContactsList = contactsView.findViewById(R.id.contacts_list);
        myContactsList.setLayoutManager(new LinearLayoutManager(getContext()));
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        contactsReference = FirebaseDatabase.getInstance().getReference()
                .child("Contacts").child(currentUserID);
        usersReference = FirebaseDatabase.getInstance().getReference()
                .child("Users");
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(contactsReference, Contacts.class)
                .build();

        final FirebaseRecyclerAdapter<Contacts, ContactsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ContactsViewHolder holder, int position, @NonNull Contacts model) {
                        final String userIDs = getRef(position).getKey();

                        usersReference.child(userIDs).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String profileStatus = snapshot.child("status")
                                            .getValue().toString();
                                    String profileUsername = snapshot.child("name")
                                            .getValue().toString();
                                    holder.username.setText(profileUsername);
                                    holder.userStatus.setText(profileStatus);

                                    if (snapshot.hasChild("image")) {
                                        String userImage = snapshot.child("image")
                                                .getValue().toString();
                                        Picasso.get().load(userImage)
                                                .placeholder(R.drawable.profile_image)
                                                .into(holder.profileImage);
                                    } else {
                                        holder.profileImage.setImageResource(R.drawable.profile_image);
                                    }

                                    if (snapshot.hasChild("userState")) {
                                        String state = snapshot.child("userState")
                                                .child("state").getValue().toString();
                                        if (state.equals("online")) {
                                            holder.onlineIcon.setVisibility(View.VISIBLE);
                                        } else {
                                            holder.onlineIcon.setVisibility(View.INVISIBLE);
                                        }
                                    } else {
                                        holder.onlineIcon.setVisibility(View.INVISIBLE);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.user_item_layout,
                                        parent, false);
                        ContactsViewHolder viewHolder = new ContactsViewHolder(view);
                        return viewHolder;
                    }
                };
        myContactsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class ContactsViewHolder extends RecyclerView.ViewHolder {
        TextView username, userStatus;
        CircleImageView profileImage;
        ImageView onlineIcon;
        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.user_profile_image);
            onlineIcon = itemView.findViewById(R.id.user_online_status);
        }
    }
}