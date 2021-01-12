package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class BoardActivity extends AppCompatActivity {
    final int CREATE = 0, UPDATE = 1, NEED_REFRESH = 3;
    RecyclerView photoRecycler;
    PhotoAdapter photoAdapter;
//    CloudBlobContainer container;
    String backEnd = "http://52.231.31.30:8000";
    int page = 1, loaded = 0;
    Boolean isEnd = false;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bord);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        //GET으로 게시물 목록 받아오기, id랑 파일이름만 필요 O
        //blob storage에서 사진 다운로드 O

        photoRecycler = findViewById(R.id.recycler_photo);
        photoAdapter = new PhotoAdapter(this, R.layout.layout_photo_item);
        photoRecycler.setAdapter(photoAdapter);

        GridLayoutManager manager = new GridLayoutManager(this, 3);
        photoRecycler.setLayoutManager(manager);

//        LinearLayoutManager manager = new LinearLayoutManager(this);
//        manager.setOrientation(LinearLayoutManager.VERTICAL);
//        photoRecycler.setLayoutManager(manager);

//        //더미 데이터
//        for(int i=0; i<30; i++){
//            Photo photo = new Photo(i, R.drawable._25102);
//            photoAdapter.addItem(photo);
//        }
//        photoAdapter.notifyDataSetChanged();

        photoRecycler.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View view, int i, int i1, int i2, int i3) {
                int totalItemCount = manager.getItemCount();
                int lastVisible = manager.findLastCompletelyVisibleItemPosition();
                if(lastVisible >= totalItemCount-1){
                    if(isEnd){
                        return;
                    }
                    //스크롤에 오류가 생겨서 전 페이지가 로딩 완료된 후에 다음 페이지가 로딩되도록 싱크 맞춤
                    if(page == loaded){
                        page++;
                        Log.d("page", page+"");
                        PhotoGetThread thread = new PhotoGetThread();
                        thread.start();
                    }
                }
            }
        });

        FloatingActionButton btnWrite = findViewById(R.id.btn_write);
        btnWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BoardActivity.this, WriteActivity.class);
                intent.putExtra("operation", CREATE);
                startActivityForResult(intent, 101);
            }
        });

//        String connectionString = getString(R.string.connection_string);
//        String containerName = getString(R.string.container_name);
//        CloudStorageAccount storageAccount = null;
//        try {
//            storageAccount = CloudStorageAccount.parse(connectionString);
//            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
//            container = blobClient.getContainerReference(containerName);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        PhotoGetThread thread = new PhotoGetThread();
        thread.start();
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
        if(resultCode==NEED_REFRESH) {
            loaded = 0;
            page = 1;
            isEnd = false;
            photoAdapter.clearItems();
            photoAdapter.notifyDataSetChanged();
            PhotoGetThread thread = new PhotoGetThread();
            thread.start();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAffinity(BoardActivity.this);
    }

    class PhotoGetThread extends Thread {
        @Override
        public void run() {
            super.run();
            //HttpURLConnection: 전에 쓰던 방법이다 실제로 잘 안 씀. 이번엔 OKHttp 라이브러리를 사용함.
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            //127.0.0.1이면 로컬이어서 에뮬레이터 자신이다.
            //10.0.2.2을 쓰면 에뮬레이터 돌아가고 있는 컴퓨터 서버로 연결됨.
            String url = backEnd+"/photo/?page="+page;
            builder = builder.url(url);
            Request request = builder.build(); //클라이언트로 보낼 request 만들기 끝

            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack); //요청에 대한 응답이 오면 callback이 실행됨.
        }

        class GetCallBack implements Callback {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("Rest", e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                //photoAdapter.clearItems();
                String result = response.body().string();
                try {
                    JSONObject root = new JSONObject(result);
                    int count = root.getInt("count");
                    if(count==0){
                        isEnd = true;
                        return;
                    }
                    JSONArray photos = root.getJSONArray("photos");
                    for(int i=0; i< photos.length(); i++){
                        JSONObject item = photos.getJSONObject(i);
                        int b_id = item.getInt("b_id");
                        String image = item.getString("image");

                        Photo photo = new Photo(b_id, image);
                        photoAdapter.addItem(photo);
                        //downloadImage(image, photoAdapter.getItemCount()-1);
//                        CloudBlockBlob blob = container.getBlockBlobReference(image);
//                        if(blob.exists()){
//                            blob.downloadAttributes();
//                            ByteArrayOutputStream os = new ByteArrayOutputStream();
//                            //blob > byte > bitmap
//                            blob.download(os);
//                            byte[] buffer = os.toByteArray();
//                            //Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
//                            Photo photo = new Photo(b_id, image, buffer);
//                            photoAdapter.addItem(photo);
//                            photoRecycler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    photoAdapter.notifyDataSetChanged();
//                                }
//                            });
//                        }
//                        Photo photo = new Photo(b_id, image, null);
//                        photoAdapter.addItem(photo);
                    }

//                    photoRecycler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            photoAdapter.notifyDataSetChanged();
//                        }
//                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //Log.d("Rest", result);
                loaded++;
            }
        }
    }

//    void downloadImage(String filename, int position){
//        Thread thread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    CloudBlockBlob blob = container.getBlockBlobReference(filename);
//                    if(blob.exists()){
//                        blob.downloadAttributes();
//                        ByteArrayOutputStream os = new ByteArrayOutputStream();
//                        //blob > byte > bitmap
//                        blob.download(os);
//                        byte[] buffer = os.toByteArray();
//                        //Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
//                        photoAdapter.updateImage(buffer, position);
////                        photoRecycler.post(new Runnable() {
////                            @Override
////                            public void run() {
////                                photoAdapter.notifyDataSetChanged();
////                            }
////                        });
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        thread.start();
//    }
}