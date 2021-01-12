package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
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

public class SignUpActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    String backEnd = "http://52.231.31.30:8000";
    String user_id, nickname, password = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mAuth = FirebaseAuth.getInstance();

        EditText edtId = findViewById(R.id.edt_txt_id_signup);
        EditText edtPass = findViewById(R.id.edt_txt_pwd_signup);
        EditText edtNick = findViewById(R.id.edt_txt_nick_signup);

        Button btnSignUp = findViewById(R.id.btn_signup);
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                user_id = edtId.getText().toString();
                password = edtPass.getText().toString();
                nickname = edtNick.getText().toString();
                if(user_id.equals("")){
                    Toast.makeText(SignUpActivity.this, "아이디를 입력하세요", Toast.LENGTH_SHORT).show();
                } else if(password.equals("")){
                    Toast.makeText(SignUpActivity.this, "비밀번호를 입력하세요", Toast.LENGTH_SHORT).show();
                } else if(nickname.equals("")){
                    Toast.makeText(SignUpActivity.this, "닉네임을 입력하세요", Toast.LENGTH_SHORT).show();
                } else{
                    //닉네임 중복인지 확인
                    NickGetThread thread = new NickGetThread();
                    thread.start();
                }
            }
        });
    }

    class NickGetThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            String url = backEnd+"/user/?nickname="+nickname;
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
                                Toast.makeText(SignUpActivity.this, "이미 존재하는 닉네임입니다.", Toast.LENGTH_SHORT).show();
                            } else{
                                //회원가입 시도
                                createUser(user_id, password);
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

    void createUser(String id, String password){
        mAuth.createUserWithEmailAndPassword(id, password).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d("SignUp", "createUserWithEmail:success");
                    UserPostThread thread = new UserPostThread();
                    thread.start();
                } else {
                    // If sign in fails, display a message to the user.
                    Exception exc = task.getException();
                    String toastMsg = "";
                    if (exc.getMessage().contains("The email address is badly formatted.")) {
                        toastMsg = "아이디를 이메일 형식으로 입력하세요";
                    } else if (exc.getMessage().contains("The given password is invalid. [ Password should be at least 6 characters ]")) {
                        toastMsg = "비밀번호를 6자리 이상 입력하세요";
                    } else if (exc.getMessage().contains("The email address is already in use by another account.")) {
                        toastMsg = "이미 등록된 이메일입니다";
                    }
                    Log.w("SignUp", "signInWithEmail:failed", task.getException());
                    Toast.makeText(SignUpActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    class UserPostThread extends Thread {
        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("user_id", user_id)
                    .add("nickname", nickname)
                    .add("state", "정상")
                    .build();
            String url = backEnd+"/user/";
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
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
                                Toast.makeText(SignUpActivity.this, "회원가입 완료", Toast.LENGTH_SHORT).show();
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