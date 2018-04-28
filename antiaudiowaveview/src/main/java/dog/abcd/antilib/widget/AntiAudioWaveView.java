package dog.abcd.antilib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * 实时显示音频时域图
 *
 * @author Michael Lee
 */
public class AntiAudioWaveView extends View {
    private static final String TAG = "AntiAudioWaveView";
    AreaBuffer areaBuffer;
    Paint paint;
    /**
     * 绘制的宽度，越大越消耗性能
     */
    int audioSampleNum = 16000;
    /**
     * 每一次绘制的大小，越小效果越平滑，但是数据实时性低，而且慢
     */
    int readCount = 800;
    /**
     * 精确度，1为最优，越大，图形越模糊
     */
    int accuracy = 1;
    int widthPixels;
    int heightPixels;
    Bitmap bitmapCache;
    DrawThread drawThread;

    public AntiAudioWaveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AntiAudioWaveView);
        int color = typedArray.getColor(R.styleable.AntiAudioWaveView_waveColor, Color.parseColor("#BF1ae8c9"));
        int strokeWidth = (int) typedArray.getDimension(R.styleable.AntiAudioWaveView_waveWidth, 1);
        audioSampleNum = typedArray.getInteger(R.styleable.AntiAudioWaveView_pointTotal, audioSampleNum);
        accuracy = typedArray.getInteger(R.styleable.AntiAudioWaveView_accuracy, accuracy);
        readCount = typedArray.getInteger(R.styleable.AntiAudioWaveView_readCount, readCount);
        areaBuffer = new AreaBuffer(audioSampleNum);
        paint = new Paint();
        paint.setColor(color);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidth);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                widthPixels = getWidth();
                heightPixels = getHeight();
            }
        });
    }

    public void putAudioData(short[] data) {
        areaBuffer.put(data);
    }

    public void startShow() {
        if (drawThread == null) {
            drawThread = new DrawThread();
            drawThread.start();
        }
    }

    public void stopShow() {
        if (drawThread != null) {
            try {
                drawThread.interrupt();
            } catch (Exception e) {
            } finally {
                drawThread = null;
            }
        }
        if (bitmapCache != null) {
            bitmapCache.recycle();
        }
    }

    class DrawThread extends Thread {

        @Override
        public void run() {
            while (!isInterrupted()) {
                short[] data = areaBuffer.get(readCount);
                drawBitmap(data);
            }
        }
    }

    private void drawBitmap(short audio[]) {
        if (widthPixels == 0 || heightPixels == 0) {
            return;
        }
        if (audio == null) {
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        float moveDistance = (float) audio.length / audioSampleNum * widthPixels;
        //往左边移动audio长度一样的宽度
        if (this.bitmapCache != null && !this.bitmapCache.isRecycled()) {
            canvas.drawBitmap(this.bitmapCache, -moveDistance, 0, paint);
        }
        //把新的线条画到最右边
        float[] pointAdd = new float[audio.length * 4];
        for (int i = 0; i < audio.length - 1; i += accuracy) {
            pointAdd[4 * i] = (float) i / audioSampleNum * widthPixels + widthPixels - moveDistance;//本来的比例，再加上左边被移动的距离
            pointAdd[4 * i + 1] = heightPixels / 2 + (float) audio[i] / 32768 * heightPixels / 2;
            pointAdd[4 * i + 2] = (float) (i + 1) / audioSampleNum * widthPixels + widthPixels - moveDistance;
            pointAdd[4 * i + 3] = heightPixels / 2 + (float) audio[i + 1] / 32768 * heightPixels / 2;
        }
        canvas.drawLines(pointAdd, paint);
        //保存上一帧的Bitmap用作下一帧的缓存
        this.bitmapCache = bitmap;
        canvas.save();
        canvas.restore();
        post(new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        });
    }

    protected void onDraw(final Canvas canvas) {
        if (bitmapCache != null && !bitmapCache.isRecycled()) {
            canvas.drawBitmap(bitmapCache, 0, 0, null);
        }
    }
}


/**
 * 这下面是最开始，最简单粗暴的方式，在性能好的手机上还能用，性能差的手机上就没法用了，有兴趣的可以研究一下，实测s8+上面宽度32000，绘制一次5毫秒
 */
//    LinkedList<float[]> pointArray = new LinkedList<>();
//    float[] points = new float[pointArray.size() * 4];
//
//    protected void drawWave(Canvas canvas, short audio[]) {
//        if (audio == null) {
//            audio = new short[0];
//        }
//        //先计算Y轴
//        for (int i = 0; i < audio.length - 1; i += accuracy) {
//            float[] floats = new float[]{
//                    0f,
//                    heightPixels / 2 + (float) audio[i] / 32768 * heightPixels / 2,
//                    0f,
//                    heightPixels / 2 + (float) audio[i + 1] / 32768 * heightPixels / 2
//            };
//            pointArray.add(floats);
//        }
//        //从头部去掉超出的部分
//        int overSize = pointArray.size() - audioSampleNum;
//        if (overSize > 0) {
//            for (int i = 0; i < overSize; i++) {
//                pointArray.removeFirst();
//            }
//        }
//        //遍历拼接成去canvas绘制线条
//        float[] floats;
//        int index = 0;
//        for (Iterator<float[]> iterator = pointArray.iterator(); iterator.hasNext(); ) {
//            floats = iterator.next();
//            floats[0] = (float) index / audioSampleNum * widthPixels;
//            floats[2] = (float) (index + 1) / audioSampleNum * widthPixels;
//            if (index * 4 >= points.length) {
//                break;
//            }
//            points[4 * index] = floats[0];
//            points[4 * index + 1] = floats[1];
//            points[4 * index + 2] = floats[2];
//            points[4 * index + 3] = floats[3];
//            index++;
//        }
//        canvas.drawLines(points, paint);
//    }