package com.eso.socialmedia.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.model.Users;
import com.eso.socialmedia.ui.activity.ChatActivity;
import com.eso.socialmedia.ui.activity.ThereProfileActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.HashMap;
import java.util.List;

import static com.eso.socialmedia.utils.Common.BLOCKED_USERS;
import static com.eso.socialmedia.utils.Common.HISUID;
import static com.eso.socialmedia.utils.Common.UID;
import static com.eso.socialmedia.utils.Common.USERS;


public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private List<Users> userList;
    private Context context;
    private String myUid;

    public UsersAdapter(Context context, List<Users> userList) {
        this.context = context;
        this.userList = userList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_users, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Users users = userList.get(position);
        String hisUid = users.getUid();
        holder.mNameTV.setText(users.getName());
        try {
            Glide.with(context).load(users.getImage()).placeholder(R.drawable.profile).into(holder.mAvatarIV);
        }catch (Exception e){
            Glide.with(context).load(R.drawable.profile).into(holder.mAvatarIV);
        }
        holder.blockIv.setImageResource(R.drawable.ic_check_circle_black_24dp);
        checkIsBlocked(hisUid,holder,position);
        holder.itemView.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setItems(new String[]{"Profile", "Chat"}, (dialog, which) -> {
                if (which == 0){
                    Intent intent = new Intent(context, ThereProfileActivity.class);
                    intent.putExtra(UID,hisUid);
                    context.startActivity(intent);
                }else if (which == 1){
                    imBlockedOrNot(hisUid);
                }
            });
            builder.show();

        });
        holder.blockIv.setOnClickListener(v -> {
            if (userList.get(position).isBlocked())
                unBlockUser(hisUid);
            else
                blockUser(hisUid);
        });
    }

    private void imBlockedOrNot(String hisUid){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(hisUid).child(BLOCKED_USERS).orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                            if (ds.exists()){
                                Toast.makeText(context, "You're blocked by that user, can't send message", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        // not Blocked
                        Intent intent = new Intent(context, ChatActivity.class);
                        intent.putExtra(HISUID,hisUid);
                        context.startActivity(intent);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void checkIsBlocked(String hisUid, ViewHolder holder, int position) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(myUid).child(BLOCKED_USERS).orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot ds : dataSnapshot.getChildren()){
                            if (ds.exists()){
                                holder.blockIv.setImageResource(R.drawable.ic_block_black_24dp);
                                userList.get(position).setBlocked(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void unBlockUser(String hisUid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(myUid).child(BLOCKED_USERS).orderByChild("uid").equalTo(hisUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    if (ds.exists()){
                        ds.getRef().removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(context, "Unblocked Successfully", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void blockUser(String hisUid) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid",hisUid);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(USERS);
        ref.child(myUid).child(BLOCKED_USERS).child(hisUid).setValue(hashMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(context, "Blocked Successfully", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        if (userList == null) return 0;
        return userList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        CircularImageView mAvatarIV;
        TextView mNameTV;
        ImageView blockIv;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            mAvatarIV = itemView.findViewById(R.id.avatarIV);
            mNameTV = itemView.findViewById(R.id.nameTV);
            blockIv = itemView.findViewById(R.id.blockIv);
        }
    }

}
