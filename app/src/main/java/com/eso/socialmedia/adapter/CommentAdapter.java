package com.eso.socialmedia.adapter;

import android.content.Context;
import android.content.DialogInterface;
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
import com.eso.socialmedia.model.Comment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.eso.socialmedia.utils.Common.COMMENTS;
import static com.eso.socialmedia.utils.Common.POSTS;


public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.viewHolder> {

    private List<Comment> commentList;
    private Context context;
    private String myUid,postId;

    public CommentAdapter( Context context,List<Comment> commentList, String myUid, String postId) {
        this.commentList = commentList;
        this.context = context;
        this.myUid = myUid;
        this.postId = postId;
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new viewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comments, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Comment comment = commentList.get(position);
        String uid = comment.getUid();
        String cid = comment.getcId();
        String email = comment.getuEmail();

        holder.mConameTv.setText(comment.getuName());
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(comment.getTimestamp()));
        String time = DateFormat.format("dd/MM/yyyy hh:mm aa", calendar).toString();
        holder.mCotimeTv.setText(time);
        Glide.with(context).load(comment.getuDp()).placeholder(R.drawable.profile).into(holder.mCoavatarIv);
        holder.mCocommentTv.setText(comment.getComment());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myUid.equals(uid)){
                    new AlertDialog.Builder(context).setTitle("Delete")
                            .setMessage("Are you sure to delete this comment?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    deleteComment(cid);
                                }
                            }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
                }else {
                    Toast.makeText(context, "Can't delete other's comment...", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void deleteComment(String cId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(POSTS).child(postId);
        ref.child(COMMENTS).child(cId).removeValue();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String comments = dataSnapshot.child("pComments").getValue(String.class);
                int newCommentVal = Integer.parseInt(comments) - 1;
                ref.child("pComments").setValue("" + newCommentVal);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        if (commentList == null) return 0;
        return commentList.size();
    }

    class viewHolder extends RecyclerView.ViewHolder {

        CircularImageView mCoavatarIv;
        TextView mConameTv,mCotimeTv,mCocommentTv;

        viewHolder(@NonNull View itemView) {
            super(itemView);
            mCoavatarIv = itemView.findViewById(R.id.coavatarIv);
            mConameTv = itemView.findViewById(R.id.conameTv);
            mCocommentTv = itemView.findViewById(R.id.cocommentTv);
            mCotimeTv = itemView.findViewById(R.id.cotimeTv);
        }
    }

}
