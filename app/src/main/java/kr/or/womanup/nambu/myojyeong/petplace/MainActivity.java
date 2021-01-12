package kr.or.womanup.nambu.myojyeong.petplace;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton btnHospital = findViewById(R.id.btn_hospital);
        ImageButton btnPharmacy = findViewById(R.id.btn_pharmacy);
        ImageButton btnActivity = findViewById(R.id.btn_activity);

        btnHospital.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SearchFacilityActivity.class);
                intent.putExtra("category","h"); //병원
                startActivityForResult(intent,101);
            }
        });
        btnPharmacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SearchFacilityActivity.class);
                intent.putExtra("category","p"); //약국
                startActivityForResult(intent,102);
            }
        });
        btnActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this,SearchFacilityActivity.class);
                intent.putExtra("category","a"); //활동시설
                startActivityForResult(intent,103);
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
        Intent intent;
        switch(id){
            case R.id.board_menu:
                intent = new Intent(this, BoardActivity.class);
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
    public void onBackPressed() {
        super.onBackPressed();
        ActivityCompat.finishAffinity(MainActivity.this);
    }
}