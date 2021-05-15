package com.github.portfolio.heyapp.Activities;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.portfolio.heyapp.R;
import com.squareup.picasso.Picasso;

/*
    In this activity, we show the user an image using a URL.

    В этой Активности мы показываем пользователю изображение, используя URL-адрес.
*/

public class ImageViewerActivity extends AppCompatActivity {

    private String imageURL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        ImageView imageView = findViewById(R.id.image_viewer);
        if (imageURL == null) {
            imageURL = getIntent().getStringExtra("url");
        }
        Picasso.get().load(imageURL).placeholder(R.drawable.crop_image_menu_flip).into(imageView);
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (imageURL != null) {
            outState.putString("URL", imageURL);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        savedInstanceState.getString("URL");
    }
}