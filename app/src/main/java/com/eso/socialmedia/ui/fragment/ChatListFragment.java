package com.eso.socialmedia.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.eso.socialmedia.R;
import com.eso.socialmedia.adapter.ChatListAdapter;
import com.eso.socialmedia.model.Chat;
import com.eso.socialmedia.model.ChatList;
import com.eso.socialmedia.model.Users;
import com.eso.socialmedia.ui.activity.MainActivity;
import com.eso.socialmedia.ui.activity.SettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.eso.socialmedia.utils.Common.CHATLIST;
import static com.eso.socialmedia.utils.Common.CHATS;
import static com.eso.socialmedia.utils.Common.USERS;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatListFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private RecyclerView mRecyclerView;
    private ChatListAdapter chatListAdapter;
    private List<ChatList> chatListList = new ArrayList<>();
    private List<Users> usersList = new ArrayList<>();
    private DatabaseReference reference;

    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        Toolbar toolbar = view.findViewById(R.id.toolbar_chat_list);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        if (((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Chat List");
        }
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        mRecyclerView = view.findViewById(R.id.recyclerView);
        reference = FirebaseDatabase.getInstance().getReference(CHATLIST).child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatListList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    ChatList chatList = ds.getValue(ChatList.class);
                    chatListList.add(chatList);
                }
                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadChats() {
        reference = FirebaseDatabase.getInstance().getReference(USERS);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Users users = ds.getValue(Users.class);
                    for (ChatList chatList : chatListList){
                        if (users.getUid() != null && users.getUid().equals(chatList.getId())){
                            usersList.add(users);
                            break;
                        }
                    }
                    chatListAdapter = new ChatListAdapter(getContext(),usersList);
                    mRecyclerView.setAdapter(chatListAdapter);
                    for (int i = 0 ; i < usersList.size(); i++)
                        lastMessage(usersList.get(i).getUid());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void lastMessage(String userId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(CHATS);
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String theLastMessage = "default";
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Chat chat = ds.getValue(Chat.class);
                    if (chat == null)
                        continue;
                    String sender = chat.getSender();
                    String receiver = chat.getReceiver();
                    if (sender == null || receiver == null)
                        continue;
                    if (chat.getReceiver().equals(currentUser.getUid())&&
                    chat.getSender().equals(userId)|| chat.getSender().equals(currentUser.getUid()))
                        if (chat.getType().equals("image"))
                            theLastMessage = "Sent a photo";
                        else
                            theLastMessage = chat.getMessage();
                }
                chatListAdapter.setLastMessageMap(userId,theLastMessage);
                chatListAdapter.notifyDataSetChanged();
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(getContext(), MainActivity.class));
            getActivity().finish();
        }else if (item.getItemId() == R.id.action_setting) {
            getActivity().startActivity(new Intent(getContext(), SettingsActivity.class));
        }
        return true;
    }
}
