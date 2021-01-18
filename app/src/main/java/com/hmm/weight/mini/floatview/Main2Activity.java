package com.hmm.weight.mini.floatview;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class Main2Activity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        ImageView img = findViewById(R.id.img);
        Glide.with(getApplicationContext()).load("https://vdn1.vzuu.com/SD/b8cd1062-1f55-11eb-a8e3-52e29d17314d.mp4?disable_local_cache=1&bu=pico&expiration=1610959333&auth_key=1610959333-0-0-a5e4ffa873c0a6913de38030bdc64a1e&f=mp4&v=hw")
//                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                .into(img);

        findViewById(R.id.bt_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Main2Activity.this,Main3Activity.class));
            }
        });
    }
}
