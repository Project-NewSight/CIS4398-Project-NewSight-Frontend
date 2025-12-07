package com.example.newsight;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;

public class YuvToRgbConverter {

    private final RenderScript rs;
    private final ScriptIntrinsicYuvToRGB scriptYuvToRgb;

    private Type yuvType, rgbaType;
    private Allocation inAlloc, outAlloc;

    public YuvToRgbConverter(Context context) {
        rs = RenderScript.create(context);
        scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public void yuvToRgb(Image image, Bitmap output) {
        if (yuvType == null) {
            Type.Builder yuvTypeBuilder = new Type.Builder(rs, Element.U8(rs))
                    .setX(image.getPlanes()[0].getBuffer().capacity());
            yuvType = yuvTypeBuilder.create();
            inAlloc = Allocation.createTyped(rs, yuvType, Allocation.USAGE_SCRIPT);
        }

        if (rgbaType == null) {
            Type.Builder rgbaTypeBuilder = new Type.Builder(rs, Element.RGBA_8888(rs))
                    .setX(output.getWidth())
                    .setY(output.getHeight());
            rgbaType = rgbaTypeBuilder.create();
            outAlloc = Allocation.createTyped(rs, rgbaType, Allocation.USAGE_SCRIPT);
        }

        // 拷贝 YUV 数据
        byte[] yuvBytes = new byte[image.getPlanes()[0].getBuffer().capacity()];
        image.getPlanes()[0].getBuffer().get(yuvBytes);
        inAlloc.copyFrom(yuvBytes);

        scriptYuvToRgb.setInput(inAlloc);
        scriptYuvToRgb.forEach(outAlloc);
        outAlloc.copyTo(output);
    }
}
