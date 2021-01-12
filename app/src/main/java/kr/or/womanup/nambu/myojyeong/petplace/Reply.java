package kr.or.womanup.nambu.myojyeong.petplace;

public class Reply {
    int r_id;
    String nick;
    String date;
    String content;
    int b_id;
    Boolean usersReply;

    public Reply(int r_id, String nick, String date, String content, Boolean usersReply) {
        this.r_id = r_id;
        this.nick = nick;
        this.date = date;
        this.content = content;
        this.usersReply = usersReply;
    }

    public Reply(String nick, String date, String content, int b_id) {
        this.nick = nick;
        this.date = date;
        this.content = content;
        this.b_id = b_id;
    }
}
