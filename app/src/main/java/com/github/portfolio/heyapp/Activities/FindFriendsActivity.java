package com.github.portfolio.heyapp.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.github.portfolio.heyapp.POJOs.Contacts;
import com.github.portfolio.heyapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class FindFriendsActivity extends AppCompatActivity {

    private String currentUserID;

    // Declare widgets
    private Toolbar mToolbar;
    private RecyclerView findFriendsRecyclerList;


    // Firebase
    private DatabaseReference usersReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        initializeFields();
    }

    private void initializeFields() {
        mToolbar = findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle(R.string.find_friends);

        findFriendsRecyclerList = findViewById(R.id.find_friends_recycler_list);
        findFriendsRecyclerList.setLayoutManager(new LinearLayoutManager(this));
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(usersReference, Contacts.class)
                .build();

        FirebaseRecyclerAdapter<Contacts,  FindFriendsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position, @NonNull Contacts model) {
                        holder.username.setText(model.getName());
                        holder.userStatus.setText(model.getStatus());
                        Picasso.get().load(model.getImage())
                                .placeholder(R.drawable.profile_image)
                                .into(holder.profileImage);

                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String visitorUserID = getRef(position).getKey();
                                Intent intent = new Intent(FindFriendsActivity.this,
                                        ProfileActivity.class);
                                intent.putExtra("visitorUserID", visitorUserID);
                                startActivity(intent);
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.user_item_layout,
                                parent, false);
                        FindFriendsViewHolder viewHolder = new FindFriendsViewHolder(view);
                        return viewHolder;
                    }
                };

        findFriendsRecyclerList.setAdapter(adapter);
        adapter.startListening();
        //
        updateUserStatus("online");
        //
    }

    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView username, userStatus;
        CircleImageView profileImage;
        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            profileImage = itemView.findViewById(R.id.user_profile_image);
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

        usersReference.child(currentUserID)
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
}