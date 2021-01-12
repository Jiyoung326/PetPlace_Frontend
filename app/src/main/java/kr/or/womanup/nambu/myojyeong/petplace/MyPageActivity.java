package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

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

public class MyPageActivity extends AppCompatActivity {
    private String END_POINT_BOOKMARK_GET= "http://52.231.31.30:8000/bookmark/%s";
    private String END_POINT_NICKNAME_GET= "http://52.231.31.30:8000/user?user_id=%s";
    MyPageBMAdapter markAdapter;
    RecyclerView recycler;
    private FirebaseAuth mAuth;
    private String userId;
    TextView txtNick;
    public static final int REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_page);
        //파이어베이스 객체 가져옴
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        userId = user.getEmail();

        TextView txtId = findViewById(R.id.txt_id_mypage);
        txtId.setText(userId);
        txtNick = findViewById(R.id.txt_nick_mypage);

        //닉네임 수정버튼
        ImageButton btnEdtNick = findViewById(R.id.btn_editnick_mypage);
        btnEdtNick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyPageActivity.this,EditNicknameActivity.class);
                intent.putExtra("nick",txtNick.getText().toString());
                intent.putExtra("userId",userId);
                startActivityForResult(intent,REQUEST_CODE);
            }
        });

        //로그아웃
        ImageButton btnLogout = findViewById(R.id.btn_logout_mypage);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MyPageActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        //내 글 보기 버튼
        ImageButton btnMyPhotoList = findViewById(R.id.btn_board_mypage);
        btnMyPhotoList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyPageActivity.this, MyBoardListActivity.class);
                startActivity(intent);
            }
        });
        //내 댓글 보기 버튼
        ImageButton btnMyReplyList = findViewById(R.id.btn_reply_mypage);
        btnMyReplyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyPageActivity.this, MyReplyListActivity.class);
                startActivity(intent);
            }
        });
        //즐겨찾기 목록
        recycler = findViewById(R.id.recycler_mypage);
        markAdapter = new MyPageBMAdapter(MyPageActivity.this,R.layout.layout_search_item);
        recycler.setAdapter(markAdapter);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        recycler.setLayoutManager(manager);
        DividerItemDecoration decoration =
                new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        recycler.addItemDecoration(decoration);
        NickNameGetThread nickThread = new NickNameGetThread();
        nickThread.start();
        BookMarkGetThread bmThread = new BookMarkGetThread();
        bmThread.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==REQUEST_CODE){
            if (resultCode== RESULT_OK){
                String newNick = data.getStringExtra("newNick");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtNick.setText(newNick);
                    }
                });
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAffinity(MyPageActivity.this);
    }

    //닉네임 가져오기
    class NickNameGetThread extends Thread{
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            String url = String.format(END_POINT_NICKNAME_GET, userId);
            Log.d("pet","url get:"+url);
            builder = builder.url(url);
            Request request = builder.build(); //클라이언트로 보낼 request 만들기 끝
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

                try {
                    Log.d("pet",result);
                    JSONObject data = new JSONObject(result);
                    String nick = data.getString("nickname");
                    Log.d("pet","닉네임: "+nick);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtNick.setText(nick);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    //백엔드 통신. 북마크 리스트 가져오는 용.
    class BookMarkGetThread extends Thread{
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            String url = String.format(END_POINT_BOOKMARK_GET, userId);
            Log.d("pet","url get:"+url);
            builder = builder.url(url);
            Request request = builder.build(); //클라이언트로 보낼 request 만들기 끝
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
                markAdapter.clear();

                try {
                    Log.d("pet",result);
                    JSONObject datas = new JSONObject(result);
                    JSONArray bookmarks = datas.getJSONArray("bookmark_data");
                    for(int i=0;i<bookmarks.length();i++){
                        JSONObject bookmark = bookmarks.getJSONObject(i);
                        int m_id = bookmark.getInt("m_id");
                        String user_id = bookmark.getString("user_id");
                        String f_id = bookmark.getString("f_id");
                        String state = bookmark.getString("state");
                        BookMark newBookmark = new BookMark(m_id,user_id,f_id,state);
                        markAdapter.addBookMarkItem(newBookmark);
                    }
                    JSONArray facilities = datas.getJSONArray("facilities_data");
                    for(int i=0;i<facilities.length();i++){
                        JSONObject facility = facilities.getJSONObject(i);
                        String f_id = facility.getString("f_id");
                        String title = facility.getString("title");
                        String gu = facility.getString("gu");
                        String address = facility.getString("address");
                        String tel = facility.getString("tel");
                        double latitude = facility.getDouble("latitude");
                        double longitude = facility.getDouble("longitude");
                        String state = facility.getString("state");
                        String description = facility.getString("description");

                        Facility newFac = new Facility(f_id,title,gu,address,tel,latitude,longitude,state,description);
                        markAdapter.addFacilityItem(newFac);
                    }
                    recycler.post(new Runnable() {
                        @Override //ui바꾸는 건 메인 쓰레드로 보내기
                        public void run() {
                            markAdapter.notifyDataSetChanged();
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override //메뉴
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

}