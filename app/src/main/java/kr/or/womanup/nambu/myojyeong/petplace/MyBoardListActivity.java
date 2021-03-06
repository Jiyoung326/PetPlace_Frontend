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

public class MyBoardListActivity extends AppCompatActivity {
    final int NEED_REFRESH = 3;
    private static final String END_POINT_MYBOARD_GET= "http://52.231.31.30:8000/photo?user_id=%s";
    private FirebaseAuth mAuth;
    private String userId;
    RecyclerView recyclerView;
    MyBoardAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_board_list);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        userId = user.getEmail();

        recyclerView = findViewById(R.id.recycler_myboardlist);
        adapter = new MyBoardAdapter(this,R.layout.layout_my_board_item);
        recyclerView.setAdapter(adapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(manager);
        DividerItemDecoration decoration =
                new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(decoration);

        MyBoardGetThread thread = new MyBoardGetThread();
        thread.start();
    }

    //백엔드 통신.
    class MyBoardGetThread extends Thread{
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            String url = String.format(END_POINT_MYBOARD_GET, userId);
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
                    JSONArray boards = datas.getJSONArray("photos");
                    for(int i=0;i<boards.length();i++){
                        JSONObject board = boards.getJSONObject(i);
                        int b_id = board.getInt("b_id");
                        String title = board.getString("title");
                        String image = board.getString("image");
                        String regdateStr = board.getString("regdate");
                        MyBoard newboard = new MyBoard(b_id,title,image,regdateStr);
                        adapter.addBoardItem(newboard);
                    }
                    recyclerView.post(new Runnable() {
                        @Override //ui바꾸는 건 메인 쓰레드로 보내기
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
                MyBoardGetThread thread = new MyBoardGetThread();
                thread.start();
            }
        }
    }
}