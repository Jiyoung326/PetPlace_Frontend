package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditNicknameActivity extends AppCompatActivity {
    private String END_POINT_NICK_GET= "http://52.231.31.30:8000/user/?nickname=%s";
    private String END_POINT_NICK_PUT= "http://52.231.31.30:8000/user/%s";
    String user_id,newNickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_nickname);

        Intent intent = getIntent();
        user_id=intent.getStringExtra("userId");
        String currNick = intent.getStringExtra("nick");
        TextView txtCurrNick = findViewById(R.id.txt_currnick);
        txtCurrNick.setText(currNick);

        EditText edtNick = findViewById(R.id.edt_new_nick);
        Button btnEdt = findViewById(R.id.btn_changeNick);
        btnEdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newNickname = edtNick.getText().toString();
                Log.d("pet","바꿀 닉네임"+newNickname);
                NickGetThread ngthread = new NickGetThread();
                ngthread.start();
            }
        });
    }

    class NickGetThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            String url = String.format(END_POINT_NICK_GET, newNickname);
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
                    Boolean nickExist = item.getBoolean("nick_exist");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(nickExist){
                                Toast.makeText(EditNicknameActivity.this, "이미 존재하는 닉네임입니다.", Toast.LENGTH_SHORT).show();
                            } else{
                                //닉네임 수정하기
                                NickPutThread thread = new NickPutThread();
                                thread.start();
                                //Intent intent = new Intent(EditNicknameActivity.this,MyPageActivity.class);
                                Intent intent = new Intent();
                                intent.putExtra("newNick",newNickname);
                                setResult(RESULT_OK,intent);
                                finish();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Log.d("Rest", result);
            }
        }
    }



    class NickPutThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("user_id",user_id)
                    .add("nickname", newNickname)
                    .add("state","정상")
                    .build();
            String url = String.format(END_POINT_NICK_PUT, user_id);
            Log.d("pet","put url: "+url);
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
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
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(EditNicknameActivity.this, "닉네임 수정 완료", Toast.LENGTH_SHORT).show();
                                finish();
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
}