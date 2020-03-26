package com.eso.socialmedia.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.model.Notification;
import com.eso.socialmedia.ui.activity.PostDetailsActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.eso.socialmedia.utils.Common.NOTIFICATION;
import static com.eso.socialmedia.utils.Common.USERS;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.viewHolder> {

    private Context context;
    private List<Notification>notificationList;
    private FirebaseAuth mAuth;
    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new viewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Notification model = notificationList.get(position);
        String name = model.getsName();
        String notification = model.getNotification();
        String image = model.getsImage();
        final String timestamp = model.getTimestamp();
        String senderUid = model.getsUid();
        final String pId = model.getpId();

        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            String name = ""+ds.child("name").getValue();
                            String image = ""+ds.child("image").getValue();
                            String email = ""+ds.child("email").getValue();

                            model.setsName(name);
                            model.setsEmail(email);
                            model.setsImage(image);

                            holder.nameTV.setText(name);

                            Glide.with(holder.itemView.getContext()).load(model.getsImage())
                                    .placeholder(R.drawable.profile)
                                    .into(holder.avatarIv);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        holder.NotificationTV.setText(notification);
        holder.TimeTv.setText(pTime);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(context, PostDetailsActivity.class);
                intent.putExtra("postId", pId);
                context.startActivity(intent);

            }
        });

        holder.itemView.setOnLongClickListener(v -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Are you sure to delete this notification?");
            builder.setPositiveButton("Delete", (dialog, which) -> {

                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                ref.child(mAuth.getUid()).child("Notifications").child(timestamp)
                        .removeValue().addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Notification deleted...", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show());

            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.create().show();

            return false;
        });
    }

    @Override
    public int getItemCount() {
        if (notificationList == null) return 0;
        return notificationList.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder{

        CircularImageView avatarIv;
        TextView nameTV,NotificationTV,TimeTv;
        public viewHolder(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIV);
            nameTV = itemView.findViewById(R.id.nameTv);
            NotificationTV = itemView.findViewById(R.id.notificationTv);
            TimeTv = itemView.findViewById(R.id.timeTv);

        }
    }
}
