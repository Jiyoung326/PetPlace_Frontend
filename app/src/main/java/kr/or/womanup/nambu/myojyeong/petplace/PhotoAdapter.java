package kr.or.womanup.nambu.myojyeong.petplace;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    Context context;
    ArrayList<Photo> photos;
    ArrayList<Bitmap> thumbnails;
    int layout;
    CloudBlobContainer container;

    public PhotoAdapter(Context context, int layout) {
        this.context = context;
        this.layout = layout;
        photos = new ArrayList<>();
        thumbnails = new ArrayList<>();

        String connectionString = context.getString(R.string.connection_string);
        String containerName = context.getString(R.string.container_name);
        CloudStorageAccount storageAccount = null;
        try {
            storageAccount = CloudStorageAccount.parse(connectionString);
            CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addItem(Photo photo){
        photos.add(photo);
        thumbnails.add(null);
        downloadImage(photo.filename, thumbnails.size()-1);
    }

    void downloadImage(String filename, int position){
    //void downloadImage(String filename, ImageView imageView){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    CloudBlockBlob blob = container.getBlockBlobReference("thumb-"+filename);
                    if(blob.exists()){
                        blob.downloadAttributes();
                        ByteArrayOutputStream os = new ByteArrayOutputStream();
                        //blob > byte > bitmap
                        blob.download(os);
                        byte[] buffer = os.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.length);
                        //updateImage(bitmap, position);
                        thumbnails.set(position, bitmap);
                        ((BoardActivity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyDataSetChanged();
                                //imageView.setImageBitmap(bitmap);
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

//    public void updateImage(Bitmap bitmap, int position){
//        thumbnails.set(position, bitmap);
//        ((BoardActivity)context).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                notifyDataSetChanged();
//            }
//        });
//        //notifyDataSetChanged();
//    }

    public void clearItems(){
        photos.clear();
        thumbnails.clear();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(layout, parent, false);
        ViewHolder holder = new ViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //Photo photo = photos.get(position);
        Bitmap thumbnail = thumbnails.get(position);
        holder.imageView.setImageResource(0);
        if(thumbnail != null){
            holder.imageView.setImageBitmap(thumbnail);
        }
        //downloadImage(photo.filename, holder.imageView);
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView_photo);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int pos = getAdapterPosition();
                    Photo photo = photos.get(pos);
                    Intent intent = new Intent(context, BoardDetailActivity.class);
                    intent.putExtra("b_id", photo.id);
                    //context.startActivity(intent);
                    ((BoardActivity)context).startActivityForResult(intent, 102);
                }
            });
        }
    }
}
