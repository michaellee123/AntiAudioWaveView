package dog.abcd.antilib.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Michael Lee
 */
public class AntiAudioWaveSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback {
    private static final String TAG = "AntiAudioWave";
    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private boolean mIsDrawing;
    long postDelayTime = 0;
    AreaBuffer areaBuffer;
    Paint paint;
    /**
     * 绘制的宽度，越大越消耗性能
     */
    int audioSampleNum = 160000;
    /**
     * 显示一帧需要的时间（默认一秒30帧）
     */
    int frameTime = 32;
    /**
     * 读取一次的时间（音频时间长度等于frameTime，默认为16k的音频显示30帧）
     */
    int readCount = 512;
    /**
     * 是否自动开关线程
     */
    boolean autoControl = true;
    int color = Color.RED;
    float strokeWidth = 1;
    int widthPixels;
    int heightPixels;
    Bitmap bitmapCache;
    LruCache<Long, Bitmap> lruCache;
    LinkedBlockingQueue<Long> keysQueue = new LinkedBlockingQueue<>();
    DrawThread drawThread;
    ShowThread showThread;
    boolean initialized = false;

    public AntiAudioWaveSurfaceView(Context context) {
        super(context);
        initView();
    }

    public AntiAudioWaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AntiAudioWaveSurfaceView);
        audioSampleNum = typedArray.getInteger(R.styleable.AntiAudioWaveSurfaceView_audioTotal, audioSampleNum);
        color = typedArray.getColor(R.styleable.AntiAudioWaveSurfaceView_strokeColor, color);
        strokeWidth = typedArray.getDimension(R.styleable.AntiAudioWaveSurfaceView_strokeWidth, strokeWidth);
        autoControl = typedArray.getBoolean(R.styleable.AntiAudioWaveSurfaceView_autoControl, true);
        int sampleRate = typedArray.getInteger(R.styleable.AntiAudioWaveSurfaceView_sampleRate, 16000);
        int frame = typedArray.getInteger(R.styleable.AntiAudioWaveSurfaceView_animationFrame, 30);
        frameTime = 1000 / frame;
        readCount = frameTime * sampleRate / 1000;
        initView();
    }

    private void initView() {
        setZOrderOnTop(true);
        mHolder = getHolder();
        mHolder.setFormat(PixelFormat.TRANSPARENT);//设置背景透明
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mHolder.addCallback(this);
        setFocusable(false);
        setFocusableInTouchMode(false);
        this.setKeepScreenOn(false);
        areaBuffer = new AreaBuffer(audioSampleNum);
        paint = new Paint();
        paint.setColor(color);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(strokeWidth);
        lruCache = new LruCache<>(15);
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                widthPixels = getWidth();
                heightPixels = getHeight();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initialized = true;
        if (autoControl)
            start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        initialized = false;
        if (autoControl)
            stop();
    }

    public void start() {
        mIsDrawing = true;
        if (showThread == null) {
            showThread = new ShowThread();
            showThread.start();
        }
        if (drawThread == null) {
            drawThread = new DrawThread();
            drawThread.start();
        }
    }

    public void stop() {
        mIsDrawing = false;
        if (showThread != null) {
            try {
                showThread.interrupt();
            } catch (SecurityException e) {
            } finally {
                showThread = null;
            }
        }
        if (drawThread != null) {
            try {
                drawThread.interrupt();
            } catch (SecurityException e) {
            } finally {
                drawThread = null;
            }
        }
        if (bitmapCache != null) {
            bitmapCache.recycle();
        }
        while (keysQueue.size() > 0) {
            try {
                lruCache.get(keysQueue.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class ShowThread extends Thread {
        @Override
        public void run() {
            while (mIsDrawing) {
                draw();
            }
        }
    }

    private void draw() {
        if (!initialized) {
            return;
        }
        long startTime = System.currentTimeMillis();
        try {
            mCanvas = mHolder.lockCanvas();
            mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Bitmap bitmap = null;
            while (bitmap == null) {
                long key = keysQueue.take();
                bitmap = lruCache.get(key);
                if (bitmap != null) {
                    mCanvas.drawBitmap(bitmap, 0, 0, null);
                    bitmap.recycle();
                    break;
                } else {
                    Log.e(TAG, "draw: miss once");
                }
            }
        } catch (Exception e) {
        } finally {
            if (mCanvas != null) {
                while (System.currentTimeMillis() - startTime < frameTime - postDelayTime) {
                    Thread.yield();
                }
                long postStartTime = System.currentTimeMillis();
                mHolder.unlockCanvasAndPost(mCanvas);//保证每次都将绘图的内容提交
                postDelayTime = System.currentTimeMillis() - postStartTime;
            }
        }
    }

    class DrawThread extends Thread {
        @Override
        public void run() {
            while (mIsDrawing) {
                drawBitmap(areaBuffer.get(readCount));
            }
        }
    }

    public void putAudioData(short[] data) {
        areaBuffer.put(data);
    }

    private void drawBitmap(short audio[]) {
        if (!initialized) {
            return;
        }
        if (widthPixels == 0 || heightPixels == 0 || audio == null) {
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
        for (int i = 0; i < audio.length - 1; i += 1) {
            pointAdd[4 * i] = (float) i / audioSampleNum * widthPixels + widthPixels - moveDistance;//本来的比例，再加上左边被移动的距离
            pointAdd[4 * i + 1] = heightPixels / 2 + (float) audio[i] / 32768 * heightPixels / 2;
            pointAdd[4 * i + 2] = (float) (i + 1) / audioSampleNum * widthPixels + widthPixels - moveDistance;
            pointAdd[4 * i + 3] = heightPixels / 2 + (float) audio[i + 1] / 32768 * heightPixels / 2;
        }
        canvas.drawLines(pointAdd, paint);
        //保存上一帧的Bitmap用作下一帧的缓存
        if (this.bitmapCache != null) {
            this.bitmapCache.recycle();
        }
        this.bitmapCache = bitmap;
        long time = System.currentTimeMillis();
        Bitmap lruBitmap = bitmapCache.copy(Bitmap.Config.ARGB_4444, true);
        lruCache.put(time, lruBitmap);
        keysQueue.add(time);
        canvas.save();
        canvas.restore();
    }
}