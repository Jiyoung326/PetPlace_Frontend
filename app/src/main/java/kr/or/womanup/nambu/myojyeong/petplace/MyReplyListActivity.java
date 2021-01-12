package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyReplyListActivity extends AppCompatActivity {
    final int NEED_REFRESH = 3;
    private static final String END_POINT_MYREPLY_GET= "http://52.231.31.30:8000/reply?user_id=%s";
    private FirebaseAuth mAuth;
    private String userId;
    RecyclerView recyclerView;
    MyReplyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reply_list);
        //파이어베이스 객체 가져옴
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        userId = user.getEmail();

        recyclerView = findViewById(R.id.recycler_myreplylist);
        adapter = new MyReplyAdapter(this,R.layout.layout_my_reply_item);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(manager);
        DividerItemDecoration decoration =
                new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);

        MyReplyGetThread thread = new MyReplyGetThread();
        thread.start();
    }

    //백엔드 통신.
    class MyReplyGetThread extends Thread{
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            String url = String.format(END_POINT_MYREPLY_GET, userId);
            Log.d("pet","url get:"+url);
            builder = builder.url(url);
            Request request = builder.build();
            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack); //요청에 대한 응답이 오면 callback이 실행됨.
        }

        class GetCallBack implements Callback {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("pet", e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                adapter.clear();

                try {
                    Log.d("pet",result);
                    JSONObject datas = new JSONObject(result);
                    JSONArray replies = datas.getJSONArray("replies");
                    for(int i=0;i<replies.length();i++){
                        JSONObject reply = replies.getJSONObject(i);
                        String userID = reply.getString("user_id");
                        String regdate = reply.getString("regdate");
                        String content = reply.getString("content");
                        int b_id = reply.getInt("b_id");
                        Reply newReply = new Reply(userID,regdate,content,b_id);
                        adapter.addReplyItem(newReply);
                    }

                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Override //메뉴를 만들면 실행되는 메소드
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        Intent intent;
        switch(id){
            case R.id.board_menu:
                intent = new Intent(this, BoardActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.home_menu:
                intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("result", resultCode+"");
        if(requestCode==101){
            if(resultCode==NEED_REFRESH || resultCode==RESULT_CANCELED){
                MyReplyGetThread thread = new MyReplyGetThread();
                thread.start();
            }
        }
    }
}