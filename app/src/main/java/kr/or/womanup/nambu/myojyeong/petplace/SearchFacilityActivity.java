package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SearchFacilityActivity extends AppCompatActivity {
    private static final String END_POINT_FAC_FROM_CURR= "http://52.231.31.30:8000/facilities/%s?gu=%s&lat=%f&long=%f";
    private static final String END_POINT_FAC_FROM_SEARCH = "http://52.231.31.30:8000/facilities/%s?gu=%s&qry=%s";
    private String END_POINT_BOOKMARK_GET = "http://52.231.31.30:8000/bookmark/%s?f_id=%s";
    private String END_POINT_BOOKMARK_POST = "http://52.231.31.30:8000/bookmark/";

    String[] permissionList={
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    String[] gus = {"강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
            "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
            "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구", "중랑구"};

    GoogleMap map;
    LocationManager locationManager;
    RecyclerView recycler;
    FacilityAdapter adapterFac;
    String category;
    String currGu="강남구";
    double currLat; //현재 위도
    double currLong; //현재 경도
    boolean isFromCurr=true; //true=현재 위치기준 시설 서치, false 검색창에 직접검색
    String searchGu;
    String searchQuery;

    private FirebaseAuth mAuth;
    private String userId;
    Boolean isMarked=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_facility);

        ImageView categoryImg = findViewById(R.id.imgview_category);
        category = getIntent().getStringExtra("category");
        Log.d("pet","카테고리:"+category);
        switch (category){
            case "h": break;
            case "p": categoryImg.setImageResource(R.drawable.find_pharmacy); break;
            case "a": categoryImg.setImageResource(R.drawable.find_activity); break;
        }
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        userId = user.getEmail();
        //=========지도================
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){ //버전확인
            requestPermissions(permissionList,111);
            //권한 확인 후 onRequestPermissionsResulult 메소드 실행됨
        }else {
            mapInit();
        }
        //========시설 목록====================
        recycler = findViewById(R.id.recycler_facilities);
        adapterFac = new FacilityAdapter(this,R.layout.layout_search_item);
        recycler.setAdapter(adapterFac);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(RecyclerView.VERTICAL);
        recycler.setLayoutManager(manager);

        DividerItemDecoration decoration =
                new DividerItemDecoration(this,DividerItemDecoration.VERTICAL);
        recycler.addItemDecoration(decoration);

        //=======검색(스피너, 서치)-===============
        Spinner spinner = findViewById(R.id.spinner_search);
        ArrayAdapter adapterSpinner = new ArrayAdapter(this, android.R.layout.simple_spinner_item,gus);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapterSpinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override //parent : 스피너 view:아이템 뷰
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {}

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        SearchView searchView = findViewById(R.id.searchView);
        searchView.setQueryHint("검색어를 입력하세요:D");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        adapterFac.clear();
                        searchGu=spinner.getSelectedItem().toString();
                        isFromCurr=false;
                        searchQuery=query;
                        FacilityGetThread thread = new FacilityGetThread();
                        thread.start();
                    }
                });
                thread.start();

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {return false;}
        });


    }

    @Override //권한에 대한 것이 다 실행되고 이 메소드 실행됨
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int result:grantResults){
            if(result== PackageManager.PERMISSION_DENIED){// 하나라도 권한 거부가 되면
                return;
            }
        }
        mapInit(); //다 허용됐으면 실행됨
    }

    public  void  mapInit(){
        FragmentManager fragmentManager = getSupportFragmentManager(); //fragment가져오게 조와주는 매니저
        SupportMapFragment mapFragment = //맵이 들어갈 fragment layout의 code에 정의해놓음.
                (SupportMapFragment) fragmentManager.findFragmentById(R.id.map);
        MapReadyCallback callback = new MapReadyCallback();
        mapFragment.getMapAsync(callback); //프레그먼트가 준비되면(map이 ready) callback의 onMapReady를 실행함

    }

    public  void currentLocation(){//현재 위치를 받아오는 함수
        //시스템에서 위치 관리해주는 것 요청
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        //시스템의 위치정보:  GPS, 네트워크로 가져오기 -> 둘다 안 될 경우 각각 마지막 위치 가져옴

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==
                    PackageManager.PERMISSION_DENIED){
                return;  //권한 비허용이면 끝
            }

        }
        //GPS, 네트워크의 마지막 위치 가져오기
        Location location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location location2 = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if(location1!=null){
            currGu = getCurrentGu(location1); //현재 위치의 구
            Log.d("pet","현재 구 : "+currGu);
            currLat=location1.getLatitude();
            currLong=location1.getLongitude();
            Log.d("pet","현재 위도 : "+currLat);
            Log.d("pet","현재 경도 : "+currLong);
            FacilityGetThread thread = new FacilityGetThread();
            thread.start();
            setLocation(currLat,currLong);
        }else if(location2!=null){
            currGu = getCurrentGu(location2); //현재 위치의 구
            Log.d("pet","현재 구 : "+currGu);
            currLat=location2.getLatitude();
            currLong=location2.getLongitude();
            FacilityGetThread thread = new FacilityGetThread();
            thread.start();
            setLocation(currLat,currLong);
        }

        CurrentLocationListener listener = new CurrentLocationListener();
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)==true) { //GPS가 사용가능할 때
            //10초마다, 최소 10m
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    10000, 10f, listener);
        }
        if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)==true){
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    10000,10f,listener);
        }
    }

    public void setLocation(double latitude, double longitude){
        Log.d("pet","위도 : "+latitude);
        Log.d("pet","경도 : "+longitude);

        LatLng nearPosition = null;
        if(adapterFac.getItemCount()!=0){
            Facility f = adapterFac.facilities.get(0);
            nearPosition = new LatLng(f.latitude,f.longitude);
            for(Facility fc :adapterFac.facilities) {
                //마커 추가
                MarkerOptions makerOptions = new MarkerOptions();
                makerOptions.position(new LatLng(fc.latitude, fc.longitude));
                Marker marker = map.addMarker(makerOptions);
                marker.setTag(fc);// to retrieve the marker
            }
            MarkerClickListener mcListener = new MarkerClickListener();
            map.setOnMarkerClickListener(mcListener);

        }else {
            nearPosition = new LatLng(latitude, longitude);
        }

        CameraUpdate update = CameraUpdateFactory.newLatLng(nearPosition);
        map.moveCamera(update); //해당 위치로 카메라를 옮긴다.
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15f); //맵 확대
        map.animateCamera(zoom);

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==
                    PackageManager.PERMISSION_DENIED){
                return;  //권한 비허용이면 끝
            }
        }
        map.setMyLocationEnabled(true); //권한 확인돼야함
    }
    
    //마커 클릭 리스너
    class MarkerClickListener implements GoogleMap.OnMarkerClickListener{

        @Override
        public boolean onMarkerClick(Marker marker) {
            //Toast.makeText(SearchFacilityActivity.this,"확인",Toast.LENGTH_SHORT).show();
            Facility facility = (Facility) marker.getTag();
            LayoutInflater inflater = LayoutInflater.from(SearchFacilityActivity.this);
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

            AlertDialog.Builder builder = new AlertDialog.Builder(SearchFacilityActivity.this);
            builder.setTitle("상세 정보");
            builder.setIcon(R.drawable.dialog_footprint);
            builder.setView(detailView);
            builder.setPositiveButton("닫기", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FragmentManager fragmentManager = (SearchFacilityActivity.this).getSupportFragmentManager();
                    SupportMapFragment mapFragment = (SupportMapFragment)fragmentManager.findFragmentById(R.id.map_detail);
                    fragmentManager.beginTransaction().remove(mapFragment).commit();

                    dialog.dismiss();
                }
            });

            FragmentManager fragmentManager = (SearchFacilityActivity.this).getSupportFragmentManager();
            SupportMapFragment mapFragment = (SupportMapFragment)fragmentManager.findFragmentById(R.id.map_detail);
            mapFragment.onResume();
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    LatLng latLng = new LatLng(facility.latitude,facility.longitude);
                    googleMap.addMarker(new MarkerOptions().position(latLng).title(facility.title));
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15f).build();
                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            });

            builder.show();
            return false;
        }
    }

    class CurrentLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            setLocation(location.getLatitude(),location.getLatitude()); //위치 바뀔 때마다 호출됨
        }
        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onProviderDisabled(@NonNull String provider) {}
    }

    class MapReadyCallback implements OnMapReadyCallback {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            map=googleMap; //mapFragment안에 googleMap이 들어있는데 그걸 가져옴
            currentLocation();
        }
    }

    //현재위치의 00구 얻는 메소드
    public String getCurrentGu(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(latitude,longitude, 7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }
        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";
        }
        Address address = addresses.get(0);
        String fullAddr= address.getAddressLine(0).toString();
        String[] fullAddrList = fullAddr.split(" ");
        Log.d("geo","지오코딩으로 받은 현재 주소: "+fullAddr);
        String guCurr = fullAddrList[2];
        //Log.d("geo","지오코딩 2번째 끝자리: "+guCurr.charAt(guCurr.length()-1));
        if (guCurr.charAt(guCurr.length()-1)!='구'){
            guCurr = fullAddrList[3];
        }
        return guCurr;
    }

    //백엔드와 통신
    class FacilityGetThread extends Thread{

        @Override
        public void run() {
            super.run();
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            String url;
            if (isFromCurr) {
                url = String.format(END_POINT_FAC_FROM_CURR, category, currGu, currLat, currLong);
            }else{
                url = String.format(END_POINT_FAC_FROM_SEARCH, category, searchGu,searchQuery);
            }
            Log.d("pet","url:"+url);
            builder = builder.url(url);
            Request request = builder.build(); //클라이언트로 보낼 request 만들기 끝
            GetCallBack callBack = new GetCallBack();
            Call call = client.newCall(request);
            call.enqueue(callBack); //요청에 대한 응답이 오면 callback이 실행됨.
        }

        class GetCallBack implements Callback{

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d("pet", e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String result = response.body().string();
                adapterFac.clear();

                try {
                    Log.d("pet",result);
                    JSONArray facilities = new JSONArray(result);

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
                        adapterFac.addItem(newFac);
                    }
                    recycler.post(new Runnable() {
                        @Override //ui바꾸는 건 메인 쓰레드로 보내기
                        public void run() {
                            adapterFac.notifyDataSetChanged();
                            if(adapterFac.getItemCount()!=0) {
                                Facility fac1 = adapterFac.facilities.get(0);
                                setLocation(fac1.latitude, fac1.longitude);
                            }
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
            case R.id.mypage_menu:
                intent = new Intent(this, MyPageActivity.class);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //백엔드와 통신 (북마크용)
    class BookMarkGetThread extends Thread{
        String url;
        ToggleButton btnBookmark;

        public BookMarkGetThread(String user_id,String f_id,ToggleButton btnBookmark) {
            this.url = String.format(END_POINT_BOOKMARK_GET,user_id,f_id);
            this.btnBookmark = btnBookmark;
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
                    btnBookmark.setChecked(isMarked);
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
/*    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String result = data.getStringExtra("success");
        if(result.equals("success")){
            FacilityGetThread thread = new FacilityGetThread();
            thread.start();
        }
    }*/

}