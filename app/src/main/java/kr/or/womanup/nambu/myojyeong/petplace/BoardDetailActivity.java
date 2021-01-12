package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BoardDetailActivity extends AppCompatActivity {
    final int CREATE = 0, UPDATE = 1, NEED_REFRESH = 3;
    int b_id;
    ImageView imgDetail;
    TextView txtTitleDetail, txtContentDetail, txtUser, txtDateDetail, txtLikeNum, txtReplyNum;
    TextView txtEdt, txtDel;
    ToggleButton btnLike;
    EditText edtReply;
    RecyclerView recyclerReply;
    ReplyAdapter replyAdapter;
    int likeNum=0, replyNum=0;
    Boolean like=false, photoUpdated=false, replyUpdating=false;
    String backEnd = "http://52.231.31.30:8000";
    private FirebaseAuth mAuth;
    String user_id, filename = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_detail);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user_id = user.getEmail();
        } else{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        //GET으로 게시물 세부 정보랑 댓글 목록 받아오기 O
        //blob storage에서 사진 다운로드 O

        Intent intent = getIntent();
        b_id = intent.getIntExtra("b_id", 0);

        imgDetail = findViewById(R.id.imageView_detail);
        imgDetail.setImageResource(0);
        txtTitleDetail = findViewById(R.id.txt_title_detail);
        txtContentDetail = findViewById(R.id.txt_content_detail);
        txtUser = findViewById(R.id.txt_user);
        txtDateDetail = findViewById(R.id.txt_date_detail);
        txtLikeNum = findViewById(R.id.txt_like_num);
        txtReplyNum = findViewById(R.id.txt_reply_num);

        txtEdt = findViewById(R.id.txt_edt);
        txtDel = findViewById(R.id.txt_del);
        txtEdt.setVisibility(View.INVISIBLE);
        txtDel.setVisibility(View.INVISIBLE);

        txtEdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BoardDetailActivity.this, WriteActivity.class);
                intent.putExtra("b_id", b_id);
                intent.putExtra("operation", UPDATE);
                intent.putExtra("title", txtTitleDetail.getText());
                intent.putExtra("content", txtContentDetail.getText());
                //Bitmap bitmap = ((BitmapDrawable)imgDetail.getDrawable()).getBitmap();
                //ByteArrayOutputStream stream = new ByteArrayOutputStream();
                //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                //byte[] byteArray = stream.toByteArray();
                //intent.putExtra("image", byteArray);
                intent.putExtra("filename", filename);
                startActivityForResult(intent, 101);
            }
        });

        txtDel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(BoardDetailActivity.this);
                builder.setMessage("게시글을 삭제하시겠습니까?");
                SimpleDialogListener listener = new SimpleDialogListener();
                builder.setPositiveButton("예", listener);
                builder.setNeutralButton("취소", listener);
                builder.show();
            }
        });

        //downloadImage(photo.filename);
        //Bitmap bitmap = BitmapFactory.decodeByteArray(photo.bytes, 0, photo.bytes.length);
        //imgDetail.setImageBitmap(bitmap);

        DetailGetThread detailGetThread = new DetailGetThread();
        detailGetThread.start();

        btnLike = findViewById(R.id.btn_like);
        LikeGetThread likeGetThread = new LikeGetThread();
        likeGetThread.start();
        btnLike.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(like == b){
                    return;
                }else{
                    like = b;
                    LikePostThread likePostThread = new LikePostThread();
                    likePostThread.start();
                }
            }
        });

        recyclerReply = findViewById(R.id.recycler_reply);
        replyAdapter = new ReplyAdapter(this, R.layout.layout_reply_item);
        recyclerReply.setAdapter(replyAdapter);

        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerReply.setLayoutManager(manager);

        DividerItemDecoration decoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerReply.addItemDecoration(decoration);

        //더미 데이터
//        for(int i=0; i<5; i++){
//            Reply reply = new Reply("지영", "2020-12-23", "효진아 사랑해~");
//            replyAdapter.addItem(reply);
//        }
//        replyAdapter.notifyDataSetChanged();
        ReplyGetThread replyGetThread = new ReplyGetThread();
        replyGetThread.start();

        edtReply = findViewById(R.id.edt_reply);
        ImageButton btnSubmitReply = findViewById(R.id.btn_submit_reply);
        btnSubmitReply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = edtReply.getText().toString();
                if(content.equals("")){
                    Toast.makeText(BoardDetailActivity.this, "내용을 입력하세요", Toast.LENGTH_SHORT).show();
                } else{
                    edtReply.clearFocus();
                    ReplyPostPutThread replyPostPutThread = new ReplyPostPutThread();
                    replyPostPutThread.start();
                }
            }
        });
    }

    class SimpleDialogListener implements DialogInterface.OnClickListener{
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            switch(i){
                case DialogInterface.BUTTON_POSITIVE:
                    PhotoDeleteThread thread = new PhotoDeleteThread();
                    thread.start();
                    break;
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
        switch(id){
            case R.id.home_menu:
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.mypage_menu:
                intent = new Intent(this, MyPageActivity.class);
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
            if(resultCode==NEED_REFRESH){
                photoUpdated = true;
                imgDetail.setImageResource(0);
            }
            DetailGetThread detailGetThread = new DetailGetThread();
            detailGetThread.start();
        }
    }

    @Override
    public void onBackPressed() {
        if(replyUpdating){
            edtReply.setText("");
            edtReply.clearFocus();
            replyAdapter.notifyDataSetChanged();
            replyUpdating = false;
        } else if(photoUpdated){
            Intent intent = new Intent();
            setResult(NEED_REFRESH, intent);
            finish();
        } else{
            super.onBackPressed();
        }
    }

    class DetailGetThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            String url = backEnd+"/photo/"+b_id;
            builder = builder.url(url);
            Request request = builder.build();
            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack);
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
                    JSONObject item = new JSONObject(result);
                    String photo_user_id = item.getString("user_id");
                    String title = item.getString("title");
                    String content = item.getString("content");
                    filename = item.getString("image");
                    String regdate = item.getString("regdate");
                    String nickname = item.getString("nickname");
                    downloadImage(filename);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtTitleDetail.setText(title);
                            txtContentDetail.setText(content);
                            txtUser.setText(nickname);
                            txtDateDetail.setText(regdate);
                            if(photo_user_id.equals(user_id)){
                                txtEdt.setVisibility(View.VISIBLE);
                                txtDel.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("Rest", result);
            }
        }
    }

    class LikeGetThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            String url = backEnd+"/like/"+b_id+"?user_id="+user_id;
            builder = builder.url(url);
            Request request = builder.build();
            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack);
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
                    JSONObject item = new JSONObject(result);
                    likeNum = item.getInt("count");
                    Boolean user_like = item.getBoolean("user_like");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtLikeNum.setText(likeNum+"");
                            like = user_like;
                            btnLike.setChecked(user_like);
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("Rest", result);
            }
        }
    }

    class LikePostThread extends Thread {
        @Override
        public void run() {
            super.run();
            String state;
            if(like){
                state = "정상";
            }else{
                state = "삭제";
            }
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("user_id", user_id)
                    .add("b_id", ""+b_id)
                    .add("state", state)
                    .build();
            String url = backEnd+"/like/";
            Request request = new Request.Builder()
                    .url(url)
                    .post(body) //post라서 따로 설정, 기본값은 get
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
                        JSONObject data = jsonObject.getJSONObject("data");
                        String state = data.getString("state");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(state.equals("삭제")){
                                    txtLikeNum.setText((--likeNum)+"");
                                } else if(state.equals("정상")){
                                    txtLikeNum.setText((++likeNum)+"");
                                }
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e("Rest", e.getMessage());
                }
                Log.d("Rest", result);
            }
        }
    }

    class ReplyGetThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            String url = backEnd+"/reply/"+b_id;
            builder = builder.url(url);
            Request request = builder.build();
            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack);
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
                    JSONObject root = new JSONObject(result);
                    replyNum = root.getInt("count");
                    JSONArray replies = root.getJSONArray("replies");
                    for(int i=0; i< replies.length(); i++){
                        JSONObject item = replies.getJSONObject(i);
                        int r_id = item.getInt("r_id");
                        String reply_user_id = item.getString("user_id");
                        String regdate = item.getString("regdate");
                        String content = item.getString("content");
                        String nickname = item.getString("nickname");
                        Boolean usersReply;
                        if(reply_user_id.equals(user_id)){
                            usersReply = true;
                        } else{
                            usersReply = false;
                        }
                        Reply reply = new Reply(r_id, nickname, regdate, content, usersReply);
                        replyAdapter.addItem(reply);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            replyAdapter.notifyDataSetChanged();
                            txtReplyNum.setText(replyNum+"개");
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("Rest", result);
            }
        }
    }

    class ReplyPostPutThread extends Thread {
        @Override
        public void run() {
            super.run();
            String content = edtReply.getText().toString();
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
            OkHttpClient client = new OkHttpClient();
            String url = backEnd+"/reply/";
            Request request;
            if(replyUpdating){
                Log.d("updating", ""+replyAdapter.getUpdating());
                String update_date = simpleDate.format(date);
                int r_id = replyAdapter.getRid(replyAdapter.getUpdating());
                RequestBody body = new FormBody.Builder()
                        .add("r_id", ""+r_id)
                        .add("content", content)
                        .add("update_date", update_date)
                        .build();
                request = new Request.Builder()
                        .url(url)
                        .put(body) //post라서 따로 설정, 기본값은 get
                        .build();
            } else{
                String regdate = simpleDate.format(date);
                RequestBody body = new FormBody.Builder()
                        .add("user_id", user_id)
                        .add("b_id", ""+b_id)
                        .add("content", content)
                        .add("regdate", regdate)
                        .add("state", "정상")
                        .build();
                request = new Request.Builder()
                        .url(url)
                        .post(body) //post라서 따로 설정, 기본값은 get
                        .build();
            }
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
                    if(success.equals("success")) {
                        if(replyUpdating){
                            String content = jsonObject.getString("content");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    edtReply.setText("");
                                    replyAdapter.updateContent(content, replyAdapter.getUpdating());
                                }
                            });
                            replyUpdating = false;
                        } else{
                            JSONObject data = jsonObject.getJSONObject("data");
                            int r_id = data.getInt("r_id");
                            String nickname = data.getString("nickname");
                            String regdate = data.getString("regdate");
                            String content = data.getString("content");
                            Reply reply = new Reply(r_id, nickname, regdate, content, true);
                            replyAdapter.addItem(reply);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    edtReply.setText("");
                                    replyAdapter.notifyDataSetChanged();
                                    txtReplyNum.setText((++replyNum) + "개");
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    Log.e("Rest", e.getMessage());
                }
            }
        }
    }

    class PhotoDeleteThread extends Thread {
        @Override
        public void run() {
            super.run();
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
            String update_date = simpleDate.format(date);
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("b_id", ""+b_id)
                    .add("update_date", update_date)
                    .add("state", "삭제")
                    .build();
            String url = backEnd+"/photo/";
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
                        Intent intent = new Intent();
                        intent.putExtra("success", success);
                        //삭제
                        setResult(NEED_REFRESH, intent);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(BoardDetailActivity.this, "삭제되었습니다", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        });
                    }
                } catch (JSONException e) {
                    Log.e("Rest", e.getMessage());
                }
            }
        }
    }

    void downloadImage(String filename){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String connectionString = getString(R.string.connection_string);
                    String containerName = getString(R.string.container_name);
                    CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
                    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
                    CloudBlobContainer container = blobClient.getContainerReference(containerName);
                    CloudBlockBlob blob = container.getBlockBlobReference(filename);
                    if(blob.exists()){
                        blob.downloadAttributes();
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        //blob > byte > bitmap
                        blob.download(os);
                        byte[] buffer = os.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                        imgDetail.post(new Runnable() {
                            @Override
                            public void run() {
                                imgDetail.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}