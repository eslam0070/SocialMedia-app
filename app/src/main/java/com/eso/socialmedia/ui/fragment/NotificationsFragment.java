package com.eso.socialmedia.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.NotificationAdapter;
import com.eso.socialmedia.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.eso.socialmedia.utils.Common.NOTIFICATION;
import static com.eso.socialmedia.utils.Common.USERS;

/**
 * A simple {@link Fragment} subclass.
 */
public class NotificationsFragment extends Fragment {

    RecyclerView recyclerView;
    FirebaseAuth mAuth;
    String myUid;
    List<Notification> notificationList = new ArrayList<>();
    NotificationAdapter notificationAdapter;
    public NotificationsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        getNotifications();

    }

    private void getNotifications() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(mAuth.getUid()).child("Notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        notificationList.clear();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){

                            Notification model = ds.getValue(Notification.class);

                            notificationList.add(model);
                        }

                        notificationAdapter = new NotificationAdapter(getActivity(), notificationList);

                        recyclerView.setAdapter(notificationAdapter);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
