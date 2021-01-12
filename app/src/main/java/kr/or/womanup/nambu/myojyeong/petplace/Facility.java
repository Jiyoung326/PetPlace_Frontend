package kr.or.womanup.nambu.myojyeong.petplace;

public class Facility {
    String f_id; //시설 고유ID
    String title; //시설명
    String gu; //위치 소속 구 ex) 강남구, 동대문구,,,
    String address; //주소
    String tel; //전화
    double latitude; //위도
    double longitude; //경도
    String state; //상태
    String description; //기타 세부 정보

    public Facility(String f_id, String title, String gu, String address, String tel,
                           double latitude, double longitude, String state, String description) {
        this.f_id = f_id;
        this.title = title;
        this.gu = gu;
        this.address = address;
        this.tel = tel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.state = state;
        this.description = description;
    }
}
