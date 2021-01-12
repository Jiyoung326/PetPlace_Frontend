package kr.or.womanup.nambu.myojyeong.petplace;

import java.util.Date;

public class MyBoard {
    int b_id;
    String title;
    String image;
    String regdate;

    public MyBoard(int b_id, String title, String image, String regdate) {
        this.b_id = b_id;
        this.title = title;
        this.image = image;
        this.regdate = regdate;
    }
}
