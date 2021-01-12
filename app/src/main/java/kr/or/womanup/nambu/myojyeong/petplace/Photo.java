package kr.or.womanup.nambu.myojyeong.petplace;

import android.graphics.Bitmap;

import java.io.Serializable;

public class Photo implements Serializable {
    int id;
    String filename;

    public Photo(int id, String filename) {
        this.id = id;
        this.filename = filename;
    }
}
