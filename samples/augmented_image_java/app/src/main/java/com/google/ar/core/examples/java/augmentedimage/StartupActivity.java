package com.google.ar.core.examples.java.augmentedimage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.contentful.java.cda.CDAArray;
import com.contentful.java.cda.CDAAsset;
import com.contentful.java.cda.CDAClient;
import com.contentful.java.cda.CDAEntry;
import com.contentful.java.cda.CDAResource;
import com.google.ar.core.examples.java.augmentedimage.rendering.XmlLayoutRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.internal.operators.flowable.FlowableJust;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StartupActivity extends Activity {
  private static final String TAG = StartupActivity.class.getSimpleName();

  private CDAClient contentful;

  private TextView status;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_startup);
  }

  @Override protected void onStart() {
    super.onStart();
    startWaitingAnimation();

    status = findViewById(R.id.startup_status);
    status.setText(R.string.contentful_loading);

    new AlertDialog
        .Builder(this)
        .setTitle("Update content from Contentful?")
        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
          update();
        })
        .setNegativeButton(android.R.string.no, (dialog, which) -> {
          startMain();
        })
        .show();
  }

  @SuppressWarnings("CheckResult")
  private void update() {
    contentful = CDAClient
        .builder()
        .setSpace("dg6ezrxigv9a")
        .setToken("704f8b9bba0027cbe53703df23bce8a4a2e0f2dd900d00e7d9a65068faf1b80c")
        .build();

    contentful
        .observe(CDAAsset.class)
        .all()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .subscribe(assets -> {
              updateStatus(R.string.contentful_caching_assets);
              updateCachedAssets(assets);

              contentful
                  .observe(CDAEntry.class)
                  .withContentType("card")
                  .all()
                  .subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.io())
                  .subscribe(entries -> {
                    updateStatus(R.string.contentful_updating_descriptions);
                    createDescriptions(entries);
                  });
            }
        );
  }

  private void updateStatus(@StringRes int update) {
    runOnUiThread(() -> status.setText(update));
  }

  private void updateCachedAssets(CDAArray assets) {
    final File externalDir = getExternalFilesDir(null);
    final OkHttpClient http = new OkHttpClient.Builder().build();
    if (externalDir == null) {
      Log.e(TAG, "Could not get external files dir");
      return;
    }

    for (final CDAResource resource : assets.items()) {
      if (!(resource instanceof CDAAsset)) {
        continue;
      }

      final CDAAsset asset = (CDAAsset) resource;

      cacheAsset(http, asset);
    }
  }

  private void cacheAsset(OkHttpClient http, CDAAsset asset) {
    final String assetFileName = asset.fileField("fileName");

    final Request request = new Request.Builder()
        .get()
        .url("https:" + asset.url())
        .build();

    ResponseBody body = null;
    try {
      final Response response = http.newCall(request).execute();

      if (response == null || !response.isSuccessful() || (body = response.body()) == null) {
        Log.e(TAG, "Not successfully requested asset: '" + asset.url() + "'");
        return;
      }

      final InputStream input = body.byteStream();
      final Path output = FileSystems.getDefault().getPath(getExternalFilesDir(null).getAbsolutePath(), assetFileName);

      Files.copy(input, output);
    } catch (IOException e) {
      Log.e(TAG, "Could not download asset from '" + asset.url() + "'.", e);
    } finally {
      if (body != null) {
        body.close();
      }
    }
  }

  @SuppressWarnings("CheckResult")
  private void createDescriptions(CDAArray entries) {
    new FlowableJust<List<CDAResource>>(entries.items())
        .flatMap(list -> Flowable.fromIterable(list))
        .filter(resource -> resource instanceof CDAEntry)
        .map(it -> (CDAEntry) it)
        .map(entry -> {
          final DescriptionView view = (DescriptionView) getLayoutInflater().inflate(R.layout.layout_description, null, false);
          view.update(entry);

          final CDAAsset marker = entry.getField("en-US", "ar_marker");
          final String markerName = marker.fileField("fileName");
          view.setTag(markerName.replace(".png", ""));
          return view;
        })
        .subscribeOn(AndroidSchedulers.mainThread())
        .observeOn(Schedulers.computation())
        .subscribe(view -> {
              final Bitmap bmp = XmlLayoutRenderer.renderBitmap(this, view);
              final String outputFile = getExternalFilesDir(null).getAbsolutePath() + File.separator + view.getTag() + ".webp";

              try {
                final OutputStream output = new FileOutputStream(outputFile);

                if (!bmp.compress(Bitmap.CompressFormat.WEBP, 100, output)) {
                  Log.e(TAG, "Could not save description image to file: '" + outputFile + "'");
                }
              } catch (IOException e) {
                Log.e(TAG, "Could not open output file: '" + outputFile + "'");
              } finally {
                if (bmp != null && !bmp.isRecycled()) {
                  bmp.recycle();
                }
              }
            }, throwable -> {
              Log.e(TAG, "Something was wrong while converting descriptions.", throwable);
            },
            () -> {
              updateStatus(R.string.contentful_updating_done);

              runOnUiThread(() -> {
                startMain();
              });
            });
  }

  private void startMain() {
    final Intent intent = new Intent(StartupActivity.this, AugmentedImageActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
  }

  private void startWaitingAnimation() {
    final View main = findViewById(R.id.startup_image);
    main.animate()
        .rotationYBy(360)
        .setDuration(1000)
        .withEndAction(this::startWaitingAnimation)
        .start();
  }
}
