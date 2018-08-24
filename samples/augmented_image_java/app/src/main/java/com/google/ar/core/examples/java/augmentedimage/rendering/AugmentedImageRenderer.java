/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.core.examples.java.augmentedimage.rendering;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Pose;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders an augmented image.
 */
public class AugmentedImageRenderer {
  private static final String TAG = "AugmentedImageRenderer";

  private static final float TINT_INTENSITY = 0.1f;
  private static final float TINT_ALPHA = 1.0f;
  private static final int[] TINT_COLORS_HEX = {
      0x000000, 0xF44336, 0xE91E63, 0x9C27B0, 0x673AB7, 0x3F51B5, 0x2196F3, 0x03A9F4, 0x00BCD4,
      0x009688, 0x4CAF50, 0x8BC34A, 0xCDDC39, 0xFFEB3B, 0xFFC107, 0xFF9800,
  };

  private Map<String, ObjectRenderer> renderers = new HashMap<>();
  private Map<String, ObjectRenderer> descriptors = new HashMap<>();

  private final ObjectRenderer andyRenderer = new ObjectRenderer();

  public AugmentedImageRenderer() {
  }

  public void createRendererOnGlThread(Context context, ObjectRenderer renderer, String modelPath, String texturePath) throws IOException {
    renderer.createOnGlThread(
        context, modelPath, texturePath);
    renderer.setMaterialProperties(0.0f, 3.5f, 1.0f, 6.0f);
  }

  public void createOnGlThread(Context context) throws IOException {
    final File rootPath = context.getExternalFilesDir(null);
    final Path external = FileSystems.getDefault().getPath(rootPath.getAbsolutePath());
    Files.list(external)
        .filter(it -> it.toString().contains(".obj"))
        .forEach(path -> {
              final ObjectRenderer renderer = new ObjectRenderer();
              try {
                createRendererOnGlThread(context, renderer, path.toString(), path.toString().replace(".obj", ".png"));
                final String fileName = path.getFileName().toString();
                final String key = fileName.replace(".obj", "");
                renderers.put(key, renderer);

              } catch (IOException e) {
                Log.e(TAG, "Could not render '" + path.toString() + "'", e);
              }
            }
        );

    Files.list(external)
        .filter(it -> it.toString().contains(".webp"))
        .forEach(path -> {
              final ObjectRenderer renderer = new ObjectRenderer();
              try {
                createRendererOnGlThread(context, renderer, rootPath.getAbsolutePath() + File.separator + "plane.obj", path.toString());
                final String fileName = path.getFileName().toString();
                final String key = fileName.replace(".webp", "");
                descriptors.put(key, renderer);

              } catch (IOException e) {
                Log.e(TAG, "Could not render '" + path.toString() + "'", e);
              }
            }
        );

    createRendererOnGlThread(context, andyRenderer, rootPath.getAbsolutePath() + File.separator + "andy.obj", rootPath.getAbsolutePath() + File.separator + "andy.png");
  }

  public void draw(
      float[] viewMatrix,
      float[] projectionMatrix,
      AugmentedImage augmentedImage,
      Anchor centerAnchor,
      float[] colorCorrectionRgba) {
    float[] tintColor =
        convertHexToColor(TINT_COLORS_HEX[augmentedImage.getIndex() % TINT_COLORS_HEX.length]);

    Pose anchorPose = centerAnchor.getPose();
    float scaleFactor = .4f;

    float[] modelMatrix = new float[16];
    anchorPose.toMatrix(modelMatrix, 0);

    final String completeKey = augmentedImage.getName().replace(".png", "");
    final String colorKey = completeKey.substring(completeKey.indexOf('-') + 1);

    if (renderers.containsKey(colorKey)) {
      float[] descriptionMatrix = new float[16];
      anchorPose
          .compose(Pose.makeTranslation(
              -0.5f * augmentedImage.getExtentX(),
              0.0f,
              0.0f)
          )
          .compose(Pose.makeRotation(
              (float) Math.PI / 4.0f,
              0.0f,
              0.0f,
              1.0f)
          )
          .toMatrix(descriptionMatrix, 0);
      float descriptionScaleFactor = .2f;

      draw(renderers.get(colorKey), modelMatrix, scaleFactor, viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
      draw(descriptors.get(completeKey), descriptionMatrix, descriptionScaleFactor, viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
    } else {
      draw(andyRenderer, modelMatrix, scaleFactor, viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
    }
  }

  private void draw(ObjectRenderer renderer, float[] modelMatrix, float scaleFactor, float[] viewMatrix, float[] projectionMatrix, float[] colorCorrectionRgba, float[] tintColor) {
    renderer.updateModelMatrix(modelMatrix, scaleFactor);
    renderer.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, tintColor);
  }

  private static float[] convertHexToColor(int colorHex) {
    // colorHex is in 0xRRGGBB format
    float red = ((colorHex & 0xFF0000) >> 16) / 255.0f * TINT_INTENSITY;
    float green = ((colorHex & 0x00FF00) >> 8) / 255.0f * TINT_INTENSITY;
    float blue = (colorHex & 0x0000FF) / 255.0f * TINT_INTENSITY;
    return new float[]{red, green, blue, TINT_ALPHA};
  }
}
