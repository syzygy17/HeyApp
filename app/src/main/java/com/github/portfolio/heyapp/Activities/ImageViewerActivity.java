package com.github.portfolio.heyapp.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.github.portfolio.heyapp.R;
import com.squareup.picasso.Picasso;

public class ImageViewerActivity extends AppCompatActivity {

    private ImageView imageView;
    private String imageURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        imageView = findViewById(R.id.image_viewer);
        imageURL = getIntent().getStringExtra("url");

        Picasso.get().load(imageURL).placeholder(R.drawable.crop_image_menu_flip).into(imageView);
    }
}