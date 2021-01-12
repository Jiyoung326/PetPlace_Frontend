package kr.or.womanup.nambu.myojyeong.petplace;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FacilityAdapter extends RecyclerView.Adapter<FacilityAdapter.ViewHolder> {
    private String END_POINT_BOOKMARK_GET = "http://52.231.31.30:8000/bookmark/%s?f_id=%s";
    private String END_POINT_BOOKMARK_POST = "http://52.231.31.30:8000/bookmark/";
    Context context;
    int layout;
    ArrayList<Facility> facilities;
    private FirebaseAuth mAuth;
    private String userId;
    Boolean isMarked=false;

    public FacilityAdapter(Context context, int layout) {
        this.context = context;
        this.layout = layout;
        facilities = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        userId = user.getEmail();
    }
    public void clear(){facilities.clear();}
    public void addItem(Facility facility){facilities.add(facility);}

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
        Facility facility = facilities.get(position);
        holder.txtFacName.setText(facility.title);
    }

    @Override
    public int getItemCount() {return facilities.size();}

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView txtFacName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFacName = itemView.findViewById(R.id.txt_facility_title);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    Facility facility = facilities.get(pos);

                    LayoutInflater inflater = LayoutInflater.from(context);
                    View detailView = inflater.inflate(R.layout.dialog_facility_detail,null);
                    TextView txtTitle = detailView.findViewById(R.id.txt_title_detail);
                    TextView txtAddress = detailView.findViewById(R.id.txt_address_detail);
                    TextView txtTel = detailView.findViewById(R.id.txt_tel_detail);
                    TextView txtDescription = detailView.findViewById(R.id.txt_description_detail);

                    txtTitle.setText(facility.title);
                    txtAddress.setText(facility.address);
                    txtTel.setText(facility.tel);
                    if (facility.description!=null &&facility.description!="null") {
                        txtDescription.setText(facility.description);
                    }else{
                        txtDescription.setText("");
                    }
                    //=======즐겨찾기 설정========
                    ToggleButton btnBookmark = detailView.findViewById(R.id.btn_bookmark_detail);
                    BookMarkGetThread getThread = new BookMarkGetThread(userId,facility.f_id,btnBookmark);
                    getThread.start();
                    btnBookmark.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            BookMarkPostThread postThread;
                            if(isChecked){ //즐겨찾기 체크
                                postThread = new BookMarkPostThread(userId,facility.f_id,"정상");
                            }else{//즐겨찾기 해제
                                postThread = new BookMarkPostThread(userId,facility.f_id,"해제");
                            }
                            postThread.start();
                        }
                    });

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setTitle("상세 정보");
                    builder.setIcon(R.drawable.dialog_footprint);
                    builder.setView(detailView);
                    DialogListener listener = new DialogListener();
                    builder.setPositiveButton("닫기",listener);

                    FragmentManager fragmentManager = ((SearchFacilityActivity)context).getSupportFragmentManager();
                    SupportMapFragment mapFragment = (SupportMapFragment)fragmentManager.findFragmentById(R.id.map_detail);
                    mapFragment.onResume();
                    mapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            LatLng latLng = new LatLng(facility.latitude,facility.longitude);
                            googleMap.addMarker(new MarkerOptions().position(latLng).title(facility.title));
//                            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
//                            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f),2000,null);
                            CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15f).build();
                            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }
                    });
                    builder.show();

                }

            });
        }
    }


    class DialogListener implements DialogInterface.OnClickListener{

        @Override
        public void onClick(DialogInterface dialog, int which) {
            //dialog창을 닫는다고 map이 사라지는 것이 아니라 자꾸 맵을 부르면 오류가남. 삭제 필요.
            FragmentManager fragmentManager = ((SearchFacilityActivity)context).getSupportFragmentManager();
            SupportMapFragment mapFragment = (SupportMapFragment)fragmentManager.findFragmentById(R.id.map_detail);
            fragmentManager.beginTransaction().remove(mapFragment).commit();

            dialog.dismiss();
        }
    }

    //백엔드와 통신 (북마크용)
    class BookMarkGetThread extends Thread{
        String url;
        ToggleButton btnBookMark;

        public BookMarkGetThread(String user_id,String f_id,ToggleButton btnBookMark) {
            this.url = String.format(END_POINT_BOOKMARK_GET,user_id,f_id);
            this.btnBookMark = btnBookMark;
        }

        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();

            Log.d("pet","get url:"+url);
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
                    JSONObject bookmark = new JSONObject(result);
                    Log.d("pet", "bookmark is marked? "+bookmark.getBoolean("isMarked"));
                    isMarked = bookmark.getBoolean("isMarked");
                    btnBookMark.setChecked(isMarked);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class BookMarkPostThread extends Thread{
        String user_id;
        String f_id;
        String state;

        public BookMarkPostThread(String user_id,String f_id,String state) {
            this.user_id = user_id;
            this.f_id = f_id;
            this.state = state;
        }

        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();
            Log.d("pet","post: "+user_id+f_id+state);
            RequestBody body = new FormBody.Builder()
                    .add("user_id",user_id)
                    .add("f_id",f_id)
                    .add("state",state)
                    .build();

            Request request = new Request.Builder()
                    .url(END_POINT_BOOKMARK_POST)
                    .post(body)
                    .build();

            PostCallBack callBack = new PostCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack); //실행(enqueue)하는데 응답은 callback에 해달라는 의미.

        }

        class PostCallBack implements Callback {
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
                        JSONObject data= jsonObject.getJSONObject("data");
                        String state = data.getString("state");
                        if(state.equals("정상")){
                            isMarked = true;
                        }else {
                            isMarked = false;
                        }
                    }

                } catch (JSONException e) {
                    Log.d("Rest", e.getMessage());
                }
            }
        }
    }
}
