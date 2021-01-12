package kr.or.womanup.nambu.myojyeong.petplace;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ViewHolder> {
    Context context;
    int layout;
    ArrayList<Reply> replies;
    String backEnd = "http://52.231.31.30:8000";
    int updating = -1;

    public ReplyAdapter(Context context, int layout) {
        this.context = context;
        this.layout = layout;
        replies = new ArrayList<>();
    }

    public void addItem(Reply reply){
        replies.add(reply);
    }

    public void clearItems(){ replies.clear(); }

    public int getUpdating(){
        return updating;
    }

    public int getRid(int pos){
        return replies.get(pos).r_id;
    }

    public void updateContent(String content, int pos){
        Reply reply = replies.get(pos);
        reply.content = content;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reply reply = replies.get(position);
        holder.txtReplyNick.setText(reply.nick);
        holder.txtReplyDate.setText(reply.date);
        holder.txtReplyContent.setText(reply.content);
        if(!reply.usersReply){
            holder.txtReplyEdt.setVisibility(View.INVISIBLE);
            holder.txtReplyDel.setVisibility(View.INVISIBLE);
        } else{
            holder.txtReplyEdt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updating = position;
                    holder.txtReplyDate.setText("수정 중...");
                    ((BoardDetailActivity) context).replyUpdating = true;
                    String content = holder.txtReplyContent.getText().toString();
                    ((BoardDetailActivity) context).edtReply.setText(content);
                    ((BoardDetailActivity) context).edtReply.setSelection(content.length());
                    ((BoardDetailActivity) context).edtReply.requestFocus();
                    InputMethodManager manager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.showSoftInput(((BoardDetailActivity) context).edtReply, InputMethodManager.SHOW_IMPLICIT);
                }
            });
            holder.txtReplyDel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage("댓글을 삭제하시겠습니까?");
                    SimpleDialogListener listener = new SimpleDialogListener(position);
                    builder.setPositiveButton("예", listener);
                    builder.setNeutralButton("취소", listener);
                    builder.show();
                }
            });
            holder.itemView.setBackgroundColor(Color.rgb(240,240,240));
        }
    }

    @Override
    public int getItemCount() {
        return replies.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView txtReplyNick, txtReplyDate, txtReplyContent, txtReplyEdt, txtReplyDel;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtReplyNick = itemView.findViewById(R.id.txt_reply_nick);
            txtReplyDate = itemView.findViewById(R.id.txt_reply_date);
            txtReplyContent = itemView.findViewById(R.id.txt_reply_content);
            txtReplyEdt = itemView.findViewById(R.id.txt_reply_edt);
            txtReplyDel = itemView.findViewById(R.id.txt_reply_del);
        }
    }

    class SimpleDialogListener implements DialogInterface.OnClickListener{
        int pos;
        public SimpleDialogListener(int pos) {
            this.pos = pos;
        }
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch(i){
                case DialogInterface.BUTTON_POSITIVE:
                    ReplyDeleteThread thread = new ReplyDeleteThread(pos);
                    thread.start();
                    break;
            }
        }
    }

    class ReplyDeleteThread extends Thread {
        int pos;
        public ReplyDeleteThread(int pos) {
            this.pos = pos;
        }
        @Override
        public void run() {
            super.run();
            Reply reply = replies.get(pos);
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
            String update_date = simpleDate.format(date);
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("r_id", ""+reply.r_id)
                    .add("update_date", update_date)
                    .add("state", "삭제")
                    .build();
            String url = backEnd+"/reply/";
            Request request = new Request.Builder()
                    .url(url)
                    .delete(body) //post라서 따로 설정, 기본값은 get
                    .build();
            GetCallBack callback = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callback);
        }

        class GetCallBack implements Callback {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("Rest", e.getMessage());
            }
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String success = jsonObject.getString("result");
                    if(success.equals("success")){
                        ((BoardDetailActivity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                replies.remove(pos);
                                notifyDataSetChanged();
                                ((BoardDetailActivity) context).txtReplyNum.setText(--((BoardDetailActivity) context).replyNum+"개");
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e("Rest", e.getMessage());
                }
            }
        }
    }
}
