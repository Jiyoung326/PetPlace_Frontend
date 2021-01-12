package kr.or.womanup.nambu.myojyeong.petplace;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyReplyAdapter extends RecyclerView.Adapter<MyReplyAdapter.ViewHolder> {
    Context context;
    int layout;
    ArrayList<Reply> myReplies;

    public MyReplyAdapter(Context context, int layout) {
        this.context = context;
        this.layout = layout;
        myReplies = new ArrayList<>();
    }
    public void clear(){myReplies.clear();}
    public void addReplyItem(Reply reply){myReplies.add(reply);}
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(layout,parent,false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Reply reply = myReplies.get(position);
            holder.txtContent.setText(reply.content);
            holder.txtRegdate.setText(reply.date);
    }

    @Override
    public int getItemCount() {return myReplies.size();}

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView txtContent;
        TextView txtRegdate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtContent = itemView.findViewById(R.id.txt_context_myreply_item);
            txtRegdate = itemView.findViewById(R.id.txt_regdate_myreply_item);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    Reply reply = myReplies.get(pos);
                    Intent intent = new Intent(context, BoardDetailActivity.class);
                    intent.putExtra("b_id", reply.b_id);
                    ((MyReplyListActivity)context).startActivityForResult(intent, 101);
                    //context.startActivity(intent);
                }
            });

        }
    }
}
