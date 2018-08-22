package com.google.ar.core.examples.java.augmentedimage;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.contentful.java.cda.CDAAsset;
import com.contentful.java.cda.CDAEntry;
import com.contentful.java.cda.LocalizedResource;

import static com.contentful.java.cda.image.ImageOption.https;


public class DescriptionView extends LinearLayout {
  private RequestManager glide;

  public DescriptionView(Context context) {
    super(context);
  }

  public DescriptionView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public DescriptionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public DescriptionView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  private ImageView image;
  private ProgressBar progress;
  private TextView name;
  private TextView description;

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    image = findViewById(R.id.description_image);
    progress = findViewById(R.id.description_progress);
    name = findViewById(R.id.description_name);
    description = findViewById(R.id.description_description);

    glide = Glide.with(this);
  }

  public void update(CDAEntry entry) {
    final LocalizedResource.Localizer localized = entry.localize("en-US");

    final Object marker = localized.getField("ar_marker");
    if (marker instanceof CDAAsset) {
      glide.load(((CDAAsset) marker).urlForImageWith(https())).into(image);
    }

    progress.setProgress((int) (Math.random() % 100));

    name.setText(localized.getField("name"));
    description.setText(localized.getField("description"));
  }
}
