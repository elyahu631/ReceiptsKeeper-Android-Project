package com.example.receiptskeeper.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.receiptskeeper.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<String> imageUrls = new ArrayList<>();

    // set the list of image URLs
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        notifyDataSetChanged();
    }

    // Create a new ImageViewHolder when needed
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.report, parent, false);
        return new ImageViewHolder(view);
    }

    // Bind data to the ImageViewHolder for a specific position
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Get the image URL for the current position
        String imageUrl = imageUrls.get(position);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get().load(imageUrl).into(holder.imageView);
        }
    }

    // size of list imageUrls - the amount of images
    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    // ViewHolder class to hold references to views within each item of the RecyclerView
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        // Constructor to initialize the ImageView

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
