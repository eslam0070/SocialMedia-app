package com.eso.socialmedia.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.eso.socialmedia.R;
import com.eso.socialmedia.model.Users;
import com.eso.socialmedia.ui.activity.ChatActivity;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.HashMap;
import java.util.List;


public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.viewHolder> {

    private List<Users> usersList;
    private Context context;
    private HashMap<String, String> lastMessageMap;
    private OnItemClickListener mClickListener;

    public ChatListAdapter(Context context, List<Users> usersList) {
        this.context = context;
        this.usersList = usersList;
        this.lastMessageMap = new HashMap<>();
    }

    public void setLastMessageMap(String userId,String lastMessage) {
        lastMessageMap.put(userId,lastMessage);
    }

    @NonNull
    @Override
    public viewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new viewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chatlist, parent, false));

    }

    @Override
    public void onBindViewHolder(@NonNull viewHolder holder, int position) {
        Users users = usersList.get(position);
        String hisUid = users.getUid();
        Glide.with(context).load(users.getImage()).placeholder(R.drawable.profile).into(holder.mProfileiIWIv);
        holder.mNametvc.setText(users.getName());
        String lastMessage = lastMessageMap.get(hisUid);
        if (lastMessage == null || lastMessage.equals("default"))
            holder.mLastMessageTv.setVisibility(View.GONE);
        else {
            holder.mLastMessageTv.setVisibility(View.VISIBLE);
            holder.mLastMessageTv.setText(lastMessage);
        }
        if (users.getOnlineStatus().equals("online"))
            holder.mOnlineStatusIv.setImageResource(R.drawable.online);
        else
            holder.mOnlineStatusIv.setImageResource(R.drawable.offline);

        holder.itemView.setOnClickListener(v -> {
            context.startActivity(new Intent(context, ChatActivity.class).putExtra("hisUid",hisUid));
        });
    }

    @Override
    public int getItemCount() {
        if (usersList == null) return 0;
        return usersList.size();
    }

    class viewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CircularImageView mProfileiIWIv;
        ImageView mOnlineStatusIv;
        TextView mNametvc,mLastMessageTv;

        viewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            mProfileiIWIv = itemView.findViewById(R.id.profileiIWIv);
            mOnlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            mNametvc = itemView.findViewById(R.id.nametvc);
            mLastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }

        @Override
        public void onClick(View v) {
            mClickListener.onClick(v, getAdapterPosition());
        }
    }


    public void setOnItemClickListener(OnItemClickListener clickListener) {
        mClickListener = clickListener;
    }


    public interface OnItemClickListener {
        void onClick(View view, int position);
    }
}
