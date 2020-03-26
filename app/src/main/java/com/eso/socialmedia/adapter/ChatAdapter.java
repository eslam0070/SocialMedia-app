package com.eso.socialmedia.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.model.Chat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.eso.socialmedia.utils.Common.CHATS;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    private List<Chat> chatList;
    private Context context;


    public ChatAdapter(Context context, List<Chat> chatList) {
        this.context = context;
        this.chatList = chatList;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType== MSG_TYPE_RIGHT){
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_right,parent,false);
            return new ViewHolder(view);
        }else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chat_left,parent,false);
            return new ViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chatList.get(position);
        String type = chat.getType();
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(chat.getTimestamp()));
        String dateTime = DateFormat.format("dd/MM/yyyy hh:mm aa",cal).toString();
        holder.mTextTime.setText(dateTime);
        if (type.equals("text")){
            holder.mTextContent.setVisibility(View.VISIBLE);
            holder.messageIv.setVisibility(View.GONE);
            holder.mTextContent.setText(chat.getMessage());
        }else {
            holder.mTextContent.setVisibility(View.GONE);
            holder.messageIv.setVisibility(View.VISIBLE);
            Glide.with(context).load(chat.getMessage()).into(holder.messageIv);
        }
        if(position == chatList.size() - 1)
        {
            if(chat.isSeen())
            {
                holder.mLytRead.setImageResource(R.drawable.ic_done_all);
            }
            else
            {
                holder.mLytRead.setImageResource(R.drawable.ic_done);
            }
        }
        else
        {
            holder.mLytRead.setVisibility(View.GONE);
        }

        holder.linearLayout.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Are you sure to delete this message");
            builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteMessage(position);
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.show();
        });
    }

    private void deleteMessage(int position) {
        String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String msgTimeStamp = chatList.get(position).getTimestamp();
        DatabaseReference dpRef = FirebaseDatabase.getInstance().getReference(CHATS);
        Query query = dpRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    if (ds.child("sender").getValue().equals(myUID)){
                        ds.getRef().removeValue();
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message","This message was deleted");
                        ds.getRef().updateChildren(hashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseUser fuser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fuser.getUid()))
            return MSG_TYPE_RIGHT;
        else
            return MSG_TYPE_LEFT;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView mTextTime,mTextContent;
        ImageView mLytRead,messageIv;
        CircularImageView mImage;
        LinearLayout linearLayout;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            linearLayout = itemView.findViewById(R.id.lyt_parent);
            messageIv = itemView.findViewById(R.id.sendImage);
            mTextTime = itemView.findViewById(R.id.text_time);
            mImage = itemView.findViewById(R.id.image);
            mTextContent = itemView.findViewById(R.id.text_content);
            mLytRead = itemView.findViewById(R.id.lyt_read);
        }

    }

}
