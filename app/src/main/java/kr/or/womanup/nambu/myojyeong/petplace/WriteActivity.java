package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WriteActivity extends AppCompatActivity {
    EditText edtTitle, edtContent;
    ImageView imgPreview;
    Bitmap bitmap = null;
    String backEnd = "http://52.231.31.30:8000";
    private FirebaseAuth mAuth;
    String user_id = null;
    final int CREATE = 0, UPDATE = 1, NEED_REFRESH = 3;
    int b_id = 0;
    int operation = -1;
    Boolean imageChanged = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user_id = user.getEmail();
        } else{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }

        edtTitle = findViewById(R.id.edt_title_write);
        edtContent = findViewById(R.id.edt_content_write);
        imgPreview = findViewById(R.id.img_preview);
        imgPreview.setImageResource(0);

        Intent intent = getIntent();
        b_id = intent.getIntExtra("b_id", 0);
        operation = intent.getIntExtra("operation", -1);
        if(operation == UPDATE){
            String title = intent.getStringExtra("title");
            String content = intent.getStringExtra("content");
            //byte[] byteArray = getIntent().getByteArrayExtra("image");
            //bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            String filename = intent.getStringExtra("filename");
            edtTitle.setText(title);
            edtTitle.setSelection(title.length());
            edtContent.setText(content);
            edtContent.setSelection(content.length());
            downloadImage(filename);
            //imgPreview.setImageBitmap(bitmap);
        }

        ImageButton btnAddPhoto = findViewById(R.id.btn_add_photo);
        btnAddPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, 101);
            }
        });

        ImageButton btnSubmitPhoto = findViewById(R.id.btn_submit_photo);
        btnSubmitPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = edtTitle.getText().toString();
                String content = edtContent.getText().toString();
                if(title.equals("")){
                    Toast.makeText(WriteActivity.this, "제목을 입력하세요", Toast.LENGTH_SHORT).show();
                } else if(content.equals("")){
                    Toast.makeText(WriteActivity.this, "내용을 입력하세요", Toast.LENGTH_SHORT).show();
                } else if(bitmap == null){
                    //이미지 선택하라는 toast
                    Toast.makeText(WriteActivity.this, "사진을 선택하세요", Toast.LENGTH_SHORT).show();
                } else{
                    //글제목,내용,파일이름 db에 업로드 O, 사진 blob storage에 저장 O
                    PhotoThread thread = new PhotoThread();
                    thread.start();
                }
            }
        });
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==101){
            if(resultCode==RESULT_OK){
                try {
                    InputStream stream = getContentResolver().openInputStream(data.getData());
                    bitmap = BitmapFactory.decodeStream(stream);
                    imgPreview.setImageBitmap(bitmap);
                    imageChanged = true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void upload(String filename){
        String connectionString = getString(R.string.connection_string);
        String containerName = getString(R.string.container_name);
        try {
            CloudStorageAccount storageAccount = CloudStorageAccount.parse(connectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            CloudBlobContainer container = blobClient.getContainerReference(containerName);

            //outputStream으로 bitmap을 byte로 변환 후 다시 inputStream으로 받아옴
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            int imageLength = inputStream.available();  //이미지 길이

            CloudBlockBlob imageblob = container.getBlockBlobReference(filename);
            imageblob.upload(inputStream, imageLength);

            //thumbnail 이미지
            ByteArrayOutputStream thumbOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, thumbOutputStream);
            InputStream thumbInputStream = new ByteArrayInputStream(thumbOutputStream.toByteArray());
            int thumbImageLength = thumbInputStream.available();  //이미지 길이

            CloudBlockBlob thumbImageblob = container.getBlockBlobReference("thumb-"+filename);
            thumbImageblob.upload(thumbInputStream, thumbImageLength);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class PhotoThread extends Thread {
        @Override
        public void run() {
            super.run();
            String title = edtTitle.getText().toString();
            String content = edtContent.getText().toString();
            long now = System.currentTimeMillis();
            Date date = new Date(now);
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
            OkHttpClient client = new OkHttpClient();
            Request request;
            RequestBody body;
            String url = backEnd+"/photo/";
            if(operation == CREATE){
                String regdate = simpleDate.format(date);
                UUID uuid = UUID.randomUUID();
                String filename = uuid.toString() + ".jpg";
                upload(filename);
                body = new FormBody.Builder()
                        .add("user_id", user_id)
                        .add("title", title)
                        .add("content", content)
                        .add("image", filename)
                        .add("regdate", regdate)
                        .add("state", "정상")
                        .build();
                request = new Request.Builder()
                        .url(url)
                        .post(body) //post라서 따로 설정, 기본값은 get
                        .build();
            } else{
                String update_date = simpleDate.format(date);
                if(imageChanged){
                    UUID uuid = UUID.randomUUID();
                    String filename = uuid.toString() + ".jpg";
                    upload(filename);
                    body = new FormBody.Builder()
                            .add("b_id", b_id+"")
                            .add("title", title)
                            .add("content", content)
                            .add("image", filename)
                            .add("update_date", update_date)
                            .build();
                } else{
                    body = new FormBody.Builder()
                            .add("b_id", b_id+"")
                            .add("title", title)
                            .add("content", content)
                            .add("update_date", update_date)
                            .build();
                }
                request = new Request.Builder()
                        .url(url)
                        .put(body) //post라서 따로 설정, 기본값은 get
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
                    if(success.equals("success")){
                        Intent intent = new Intent();
                        intent.putExtra("success", success);
                        if(imageChanged){
                            setResult(NEED_REFRESH, intent);
                        } else{
                            setResult(RESULT_OK, intent);
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
                        bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                        imgPreview.post(new Runnable() {
                            @Override
                            public void run() {
                                imgPreview.setImageBitmap(bitmap);
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