package com.mughalai.studio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Adds gentle cinematic camera motion and readable subtitles to generated 3D frames. */
public final class CartoonFrameRenderer {
    private CartoonFrameRenderer() {}

    public static void draw(Canvas canvas, Bitmap bitmap, String caption, int frame, boolean english) {
        int w = canvas.getWidth(), h = canvas.getHeight();
        if (bitmap != null && !bitmap.isRecycled()) {
            float zoom = 1.015f + (float)(Math.sin(frame * 0.008) + 1.0) * 0.012f;
            float bw = bitmap.getWidth() * zoom;
            float bh = bitmap.getHeight() * zoom;
            float scale = Math.max(w / bw, h / bh);
            float dw = bitmap.getWidth() * scale * zoom;
            float dh = bitmap.getHeight() * scale * zoom;
            float pan = (float)Math.sin(frame * 0.004) * w * 0.012f;
            Rect src = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            android.graphics.RectF dst = new android.graphics.RectF(
                    (w-dw)/2f + pan, (h-dh)/2f, (w+dw)/2f + pan, (h+dh)/2f);
            Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(bitmap, src, dst, imagePaint);
        } else {
            canvas.drawColor(Color.rgb(45, 80, 100));
        }

        Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
        shade.setShader(new LinearGradient(0, h*0.70f, 0, h,
                new int[]{Color.TRANSPARENT, 0xDD07131D}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, h*0.68f, w, h, shade);
        shade.setShader(null);

        if (caption != null && !caption.trim().isEmpty()) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            p.setTextSize(english ? 29 : 31);
            p.setColor(Color.WHITE);
            p.setShadowLayer(3, 1, 2, Color.BLACK);
            String text = caption.trim().replace('\n', ' ');
            float max = w - 100;
            while (p.measureText(text) > max && text.length() > 18) {
                text = text.substring(0, text.length()-1);
            }
            if (!text.equals(caption.trim())) text = text.trim() + "…";
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(text, w/2f, h-48, p);
        }
    }
}
