package com.cola.code.album.support;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * Created by yanghaoyang on 15/10/26.
 */
public class ImgLoader {

    private static ImgLoader mInstance;

    /**
     * 用来在内存中缓存Bitmap
     */
    private LruCache<String,Bitmap> mLruCache;

    /**
     * 线程池
     */
    private ExecutorService mThreadPool;

    private Semaphore mSemaphoreThreadPool;

    /**
     * 线程池维护的默认线程数
     */
    private static final int DEFAULT_THREAD_COUNT = 1;

    /**
     * 队列的调度方式
     */
    private static Type mType = Type.FILO;

    /**
     * 任务队列
     */
    private LinkedList<Runnable> mTaskQueue;

    /**
     * 后台轮巡线程
     */
    private Thread mPoolThread;
    private Handler mPoolThreadHandler;

    /**
     * 信号量，解决使用mPoolThreadHandler时有可能还没初始化的问题
     */
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0);

    /**
     * UI线程中的handler
     */
    private Handler mUIhandler;

    public enum Type {
        FIFO,FILO;
    }

    private ImgLoader(int threadCount,Type type){
        init(threadCount,type);
    }

    private void init(int threadCount, Type type) {
        // 后台轮巡线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {

                        }
                        // 线程池取出一个任务去执行
                        mThreadPool.execute(getTask());
                    }
                };
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        // 获取应用最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory/8;
        mLruCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };

        // 创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**
     * 从队列中取出一个任务
     * @return 根据队列策略，返回任务
     */
    private Runnable getTask() {
        if(mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.FILO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    /**
     * 单例模式
     * @return 单例的ImgLoader对象
     */
    public static ImgLoader getInstance(int threadCount,Type type) {
        if(mInstance == null) {
            synchronized (ImgLoader.class) {
                if (mInstance == null) {
                    mInstance = new ImgLoader(threadCount,type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 加载图片
     * @param path 图片路径
     * @param imageView 图片将要显示的ImageView
     */
    public void loadImg(final String path, final ImageView imageView) {
        // 为了防止ImageView复用和回调显示图片产生图片错乱,给imageview设置标识
        imageView.setTag(path);
        if(mUIhandler == null) {
            // 初始化UI线程的Handler，该方法在主线程中调用
            mUIhandler = new Handler(){
                @Override
                public void handleMessage(Message msg) {
                    // 获取图片，为imageview设置回调显示
                    ImgBeanHolder holder = (ImgBeanHolder) msg.obj;
                    Bitmap bm = holder.bitmap;
                    ImageView iv = holder.imageView;
                    String path = holder.path;
                    // 将path与getTag()存储的路径进行比较
                    if(iv.getTag().equals(path)) {
                        iv.setImageBitmap(bm);
                    }
                }
            };
        }
        Bitmap bm = getBitmapFromLruCache(path);
        if(bm != null) {
            refreshBitmap(path,bm,imageView);
        } else { // 内存中没有该图的缓存
            addTask(new Runnable(){
                @Override
                public void run() {
                    // 获取图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    // 压缩图片
                    Bitmap bm = compressBitmapFromPath(path,imageSize.ImageWidth,imageSize.ImageHeight);
                    // 将bitmap放入缓存
                    addBitmapToLruCache(path,bm);
                    refreshBitmap(path,bm,imageView);
                }
            });
        }
        mSemaphoreThreadPool.release();
    }

    private void refreshBitmap(String path,Bitmap bm,ImageView imageView) {
        Message message = Message.obtain();
        ImgBeanHolder holder = new ImgBeanHolder();
        holder.bitmap = bm;
        holder.imageView = imageView;
        holder.path = path;
        message.obj = holder;
        mUIhandler.sendMessage(message);
    }

    private void addBitmapToLruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path) == null) {
            if(bm != null) {
                mLruCache.put(path,bm);
            }
        }
    }

    /**
     * 根据图片需要显示的宽高对图片进行压缩
     * @param path 图片路径
     * @param imageWidth 宽
     * @param imageHeight 高
     * @return 压缩过的Bitmap
     */
    private Bitmap compressBitmapFromPath(String path, int imageWidth, int imageHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        options.inSampleSize = caculateInSampleSize(options,imageWidth,imageHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path,options);
        return bitmap;
    }

    /**
     * 根据需求计算samplesize
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int caculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if(width>reqWidth || height>reqHeight) {
            int widthRadio = Math.round(width*1.0f/reqWidth);
            int heightRadio = Math.round(height*1.0f/reqHeight);
            inSampleSize = Math.max(widthRadio,heightRadio);
        }
        return inSampleSize;
    }

    /**
     * 根据ImageView的大小获取图片适合的宽高值
     * @param imageView 图片显示的ImageView
     * @return 压缩图片的合适的宽高值
     */
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        int width = imageView.getWidth();
        if(width <= 0) { // 当imageview没有显示出来时，width = 0
            // 获取imageview在容器中声明的宽度
            width = lp.width;
        }
        if(width <= 0) { // width有可能是wrap_content或match_parent
            width = getImageViewFieldValue(imageView,"mMaxWidth");
        }
        if(width <= 0) { // 如果最终无法获取，则设置为屏幕宽度
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();
        if(height <= 0) { // 当imageview没有显示出来时，height = 0
            // 获取imageview在容器中声明的宽度
            height = lp.height;
        }
        if(height <= 0) { // wheight有可能是wrap_content或match_parent
            height = getImageViewFieldValue(imageView,"mMaxHeight");
        }
        if(width <= 0) { // 如果最终无法获取，则设置为屏幕宽度
            height = displayMetrics.heightPixels;
        }

        imageSize.ImageWidth = width;
        imageSize.ImageHeight = height;

        return imageSize;
    }

    /**
     * 通过反射获得ImageView的属性值，用来兼容低版本SDK
     * @param obj
     * @param fieldName
     * @return
     */
    private int getImageViewFieldValue(Object obj,String fieldName) {
        int value = 0;
        Field field = null;
        try {
            field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(obj);
            if(fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if(mPoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
        }
        mPoolThreadHandler.sendEmptyMessage(1);
    }

    /**
     * 根据path在缓存中获取Bitmap
     * @param path 图片的路径，这里做为Key
     * @return 对应Key的Bitmap
     */
    private Bitmap getBitmapFromLruCache(String path) {
        return mLruCache.get(path);
    }

    private class ImgBeanHolder {
        ImageView imageView;
        Bitmap bitmap;
        String path;
    }

    private class ImageSize {
        int ImageHeight;
        int ImageWidth;
    }

}
