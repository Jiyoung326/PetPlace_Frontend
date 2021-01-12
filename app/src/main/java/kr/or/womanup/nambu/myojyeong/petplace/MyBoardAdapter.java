package kr.or.womanup.nambu.myojyeong.petplace;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class MyBoardAdapter extends RecyclerView.Adapter<MyBoardAdapter.ViewHolder> {
    Context context;
    int layout;
    ArrayList<MyBoard> boards;

    public MyBoardAdapter(Context context, int layout) {
        this.context = context;
        this.layout = layout;
        boards = new ArrayList<>();
    }
    public void addBoardItem(MyBoard board){boards.add(board);}
    public void clear(){boards.clear();}
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView= inflater.inflate(layout,parent,false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MyBoard board = boards.get(position);
        holder.txtTitle.setText(board.title);
        holder.txtRegdate.setText(board.regdate);

    }

    @Override
    public int getItemCount() {return boards.size();}

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView txtTitle;
        TextView txtRegdate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txt_title_myboard);
            txtRegdate = itemView.findViewById(R.id.txt_regdate_myboard);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    MyBoard myBoard = boards.get(pos);
                    Intent intent = new Intent(context, BoardDetailActivity.class);
                    intent.putExtra("b_id", myBoard.b_id);
                    ((MyBoardListActivity)context).startActivityForResult(intent, 101);
                    //context.startActivity(intent);
                }
            });
        }
    }
}
