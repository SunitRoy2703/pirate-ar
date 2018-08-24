package contentful.augmented.reality.demo.rendering;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.View;

public class XmlLayoutRenderer {
  public static Bitmap renderBitmap(Context context, View view) {
    final DisplayMetrics displayMetrics = new DisplayMetrics();
    final DisplayManager manager = context.getSystemService(DisplayManager.class);
    manager.getDisplays()[0].getMetrics(displayMetrics);
    final int height = displayMetrics.heightPixels;
    final int width = displayMetrics.widthPixels;

    final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    final Canvas canvas = new Canvas(bitmap);

    int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
    int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

    view.measure(measuredWidth, measuredHeight);
    view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
    view.draw(canvas);

    return bitmap;
  }
}
