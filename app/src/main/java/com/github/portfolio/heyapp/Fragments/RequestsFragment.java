package com.github.portfolio.heyapp.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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


public class RequestsFragment extends Fragment {

    private String currentUserID;

    // Declare widgets
    private View requestsFragmentView;
    private RecyclerView myRequestsList;

    // Firebase
    private DatabaseReference chatRequestsReference, usersReference, getRequestTypeReference,
            contactsReference;
    private FirebaseAuth firebaseAuth;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestsFragmentView = inflater.inflate(R.layout.fragment_requests,
                container, false);
        initializeFields();
        return requestsFragmentView;
    }

    private void initializeFields() {
        myRequestsList = requestsFragmentView.findViewById(R.id.chat_requests_list);
        myRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        chatRequestsReference = FirebaseDatabase.getInstance().getReference()
                .child("Chat Requests");
        usersReference = FirebaseDatabase.getInstance().getReference()
                .child("Users");
        contactsReference = FirebaseDatabase.getInstance().getReference().child("Contacts");
    }


    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(chatRequestsReference.child(currentUserID), Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts, RequestsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, RequestsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull RequestsViewHolder holder, int position, @NonNull Contacts model) {
                        holder.itemView.findViewById(R.id.accept_request_button)
                                .setVisibility(View.VISIBLE);
                        holder.itemView.findViewById(R.id.cancel_request_button)
                                .setVisibility(View.VISIBLE);

                        final String userID = getRef(position).getKey();
                        getRequestTypeReference = getRef(position).child("request_type").getRef();
                        getRequestTypeReference.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    String requestType = snapshot.getValue().toString();
                                    if (requestType.equals("received")) {
                                        usersReference.child(userID)
                                                .addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        String profileName = snapshot.child("name")
                                                                .getValue().toString();

                                                        holder.username.setText(profileName);
                                                        holder.userStatus.setText(getString(R.string.wants_contact));

                                                        if (snapshot.hasChild("image")) {
                                                            String userImage = snapshot.child("image")
                                                                    .getValue().toString();
                                                            Picasso.get().load(userImage)
                                                                    .placeholder(R.drawable.profile_image)
                                                                    .into(holder.profileImage);
                                                        } else {
                                                            holder.profileImage
                                                                    .setImageResource
                                                                            (R.drawable.profile_image);
                                                        }

                                                        holder.username
                                                                .setOnClickListener(v -> showAlertDialog(profileName, userID));
                                                        holder.userStatus
                                                                .setOnClickListener(v -> showAlertDialog(profileName, userID));
                                                        holder.profileImage
                                                                .setOnClickListener(v -> showAlertDialog(profileName, userID));

                                                        holder.acceptButton
                                                                .setOnClickListener(v -> contactsReference
                                                                        .child(currentUserID).child(userID)
                                                                        .child("Contact").setValue("Saved").addOnCompleteListener(task -> {
                                                                            if (task.isSuccessful()) {
                                                                                contactsReference
                                                                                        .child(userID).child(currentUserID)
                                                                                        .child("Contact").setValue("Saved").addOnCompleteListener(task1 -> {
                                                                                    if (task1.isSuccessful()) {
                                                                                        deleteUserRequest(userID, "accept");
                                                                                    }
                                                                                });
                                                                            }
                                                                        }));
                                                        holder.cancelButton.setOnClickListener(v -> deleteUserRequest(userID, "decline"));
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                    } else if (requestType.equals("sent")) {
                                        Button requestSentButton = holder.itemView.findViewById(R.id.accept_request_button);
                                        requestSentButton.setText(getString(R.string.request_sent));

                                        holder.itemView.findViewById(R.id.cancel_request_button)
                                                .setVisibility(View.INVISIBLE);

                                        usersReference.child(userID)
                                                .addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        String profileName = snapshot.child("name")
                                                                .getValue().toString();

                                                        holder.username.setText(profileName);
                                                        holder.userStatus.setText(getString(R.string.you_sent_request) + profileName);

                                                        if (snapshot.hasChild("image")) {
                                                            String userImage = snapshot.child("image")
                                                                    .getValue().toString();
                                                            Picasso.get().load(userImage)
                                                                    .placeholder(R.drawable.profile_image)
                                                                    .into(holder.profileImage);
                                                        } else {
                                                            holder.profileImage
                                                                    .setImageResource
                                                                            (R.drawable.profile_image);
                                                        }

                                                        holder.itemView.setOnClickListener(v -> {
                                                            CharSequence[] options1 =
                                                                    new CharSequence[]{
                                                                            getString(R.string.cancel_chat_request)
                                                                    };
                                                            AlertDialog.Builder builder =
                                                                    new AlertDialog.Builder(getContext());
                                                            builder.setTitle(getString(R.string.cancel_request_question));
                                                            builder.setItems(options1, (dialog, which) -> {
                                                                if (which == 0) {
                                                                    deleteUserRequest(userID, "cancel");
                                                                }
                                                            });
                                                            builder.show();
                                                        });
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
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
                    public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.user_item_layout,
                                        parent,
                                        false);
                        RequestsViewHolder viewHolder = new RequestsViewHolder(view);
                        return viewHolder;
                    }
                };
        myRequestsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        TextView username, userStatus;
        CircleImageView profileImage;
        Button acceptButton, cancelButton;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.user_profile_image);
            acceptButton = itemView.findViewById(R.id.accept_request_button);
            cancelButton = itemView.findViewById(R.id.cancel_request_button);
        }
    }

    private void showAlertDialog(String profileName, String userID) {
        CharSequence[] options =
                new CharSequence[]{
                        getString(R.string.accept),
                        getString(R.string.cancel)
                };
        AlertDialog.Builder builder =
                new AlertDialog.Builder(getContext());
        builder.setTitle(profileName + getString(R.string.chat_request));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                contactsReference
                        .child(currentUserID).child(userID)
                        .child("Contact").setValue("Saved").addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contactsReference
                                .child(userID).child(currentUserID)
                                .child("Contact").setValue("Saved").addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                deleteUserRequest(userID, "accept");
                            }
                        });
                    }
                });
            }
            if (which == 1) {
                deleteUserRequest(userID, "decline");
            }
        });
        builder.show();
    }

    private void deleteUserRequest(String userID, String command) {
        chatRequestsReference.child(currentUserID).child(userID)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        chatRequestsReference.child(userID).child(currentUserID)
                                .removeValue()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        switch (command) {
                                            case "decline":
                                                Toast.makeText(getContext(),
                                                        getString(R.string.chat_request_rejected), Toast.LENGTH_SHORT).show();
                                                break;
                                            case "accept":
                                                Toast.makeText(getContext(),
                                                        getString(R.string.contact_added_saved), Toast.LENGTH_SHORT).show();
                                                break;
                                            case "cancel":
                                                Toast.makeText(getContext(),
                                                        getString(R.string.chat_request_canceled), Toast.LENGTH_SHORT).show();
                                                break;
                                        }
                                    }
                                });
                    }
                });
    }
}

