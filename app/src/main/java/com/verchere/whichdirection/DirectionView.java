package com.verchere.whichdirection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.view.TextureView;
import android.view.View;

public class DirectionView extends View {

    TextureView mTextureView;

    DirectionView(Context context){
        super(context);
    }

    void init(final TextureView textureView){
        assert textureView!=null;
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                // draw
                Canvas canvas = textureView.lockCanvas();

                Paint P = new Paint();
                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                float startY = height/2;
                float startX = width/2-100;

                float[] vertsTriangle = {startX, startY, startX +200, startY, startX +100, startY - 200};
                int[] colors = {Color.rgb(0,87,75), Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75)};
                canvas.drawVertices(Canvas.VertexMode.TRIANGLES,vertsTriangle.length,vertsTriangle,0,null,0,colors,0,null,0,0,P);

                float[] vertsRect = {startX+50, startY + 200, startX + 150, startY + 200, startX +50, startY, startX +150, startY};
                int[] colors2 = {Color.rgb(0,87,75), Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75),Color.rgb(0,87,75)};
                canvas.drawVertices(Canvas.VertexMode.TRIANGLE_STRIP,vertsRect.length,vertsRect,0,null,0,colors2,0,null,0,0,P);

                P.setARGB(255,255,255,255);
                canvas.drawLine(startX,startY,startX +100, startY -200,P);
                canvas.drawLine(startX + 200,startY,startX +100, startY -200,P);
                canvas.drawLine(startX ,startY,startX +50, startY, P);
                canvas.drawLine(startX+150 ,startY,startX +200, startY, P);
                canvas.drawLine(startX+50 ,startY,startX +50, startY+200, P);
                canvas.drawLine(startX+150 ,startY,startX +150, startY+200, P);
                canvas.drawLine(startX+50 ,startY + 200,startX +150, startY+200, P);

                textureView.unlockCanvasAndPost(canvas);


            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // Transform you image captured size according to the surface width and height
            }
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {

                return false;
            }
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
        textureView.setAlpha(0.5f); // deal with opacity
        mTextureView =textureView;
    }

    void setVisibility(boolean visibility){
        if (visibility){
            mTextureView.setVisibility(View.VISIBLE);
        }else{
            mTextureView.setVisibility(View.INVISIBLE);
        }
    }
}
