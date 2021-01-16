package com.hmm.weight.mini.floatview;

import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.floatview.R;
import com.hmm.weight.mini.floatview.weight.YeahFloatView;

public class BaseActivity extends AppCompatActivity {

    private YeahFloatView floatingView;

    @Override
    protected void onResume() {
        super.onResume();
        if (null == floatingView) {
            floatingView = new YeahFloatView(this);
            floatingView.circleImageView().setImageResource(R.mipmap.game);
            floatingView.showFloat();
            floatingView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                        Toast.makeText(BaseActivity.this, "你点击了", Toast.LENGTH_SHORT).show();


                }
            });
//            Glide.with(getApplicationContext()).load("https://p26-tt.byteimg.com/origin/pgc-image/6ffb4c43c75749dd9af820028871242b")
//                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
//                    .into(floatingView.CircleImageView());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (floatingView != null&& !Settings.canDrawOverlays(this)) {
                floatingView.dismissFloatView();
                floatingView = null;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            floatingView.dismissFloatView();
            floatingView = null;
        }
    }
}
