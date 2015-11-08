package com.cola.code.album;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cola.code.album.adapter.ImageAdapter;
import com.cola.code.album.bean.FolderBean;
import com.cola.code.album.view.ListImgDirPopupWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private GridView gv;
    // 存放要显示的图片的路径
    private List<String> mList;
    private ImageAdapter adapter;

    private ListImgDirPopupWindow popupWindow;

    private RelativeLayout rl_bottom;
    private TextView tv_dir_name;
    private TextView tv_dir_count;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();
    private ProgressDialog mProgressDialog;

    private static final int FINISH_LOAD = 1;
    private Bitmap defaultBitmap;

    private AbsListView.RecyclerListener recyclerListener = new AbsListView.RecyclerListener() {
        @Override
        public void onMovedToScrapHeap(View view) {
            Log.i("RECYCLE",view.toString());
            if(view instanceof RelativeLayout) {
                ImageView iv = (ImageView) view.findViewById(R.id.iv_item);
                if(iv != null) {
                    try {
                        BitmapDrawable drawable = (BitmapDrawable)iv.getDrawable();
                        Bitmap bmp = drawable.getBitmap();
                        if (null != bmp && !bmp.isRecycled()){
                            bmp.recycle();
                            bmp = null;
                        }
                        iv.setImageBitmap(defaultBitmap);
                    } catch (ClassCastException e) {

                    }
                }
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == FINISH_LOAD) {
                mProgressDialog.dismiss();
                data2View();
                initPopupWindow();
            }
        }
    };

    private void initPopupWindow() {
        popupWindow = new ListImgDirPopupWindow(MainActivity.this,mFolderBeans);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                 lightOn();
            }
        });
        popupWindow.setOnDirSelectedListener(new ListImgDirPopupWindow.OnDirSelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDir = new File(folderBean.getDir());
                mList = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg") || filename.endsWith("jpeg") || filename.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));
                adapter = new ImageAdapter(MainActivity.this,mList,mCurrentDir.getAbsolutePath());
                gv.setAdapter(adapter);
                tv_dir_count.setText(mList.size() + "");
                tv_dir_name.setText(folderBean.getName());
                popupWindow.dismiss();
            }
        });
    }

    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    /**
     * 绑定数据到View
     */
    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT);
            return;
        }
        mList = Arrays.asList(mCurrentDir.list());
        adapter = new ImageAdapter(this,mList,mCurrentDir.getAbsolutePath());
        gv.setRecyclerListener(recyclerListener);
        gv.setAdapter(adapter );
        tv_dir_count.setText(mMaxCount + "");
        tv_dir_name.setText(mCurrentDir.getName() );

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        defaultBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.shape_default_pic);
        initView();//初始化控件
        initDatas();//遍历文件
        initEvents();//处理点击事件
    }

    private void initEvents() {
        rl_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 popupWindow.showAsDropDown(rl_bottom,0,0);
                lightOff();
            }
        });
    }

    /**
     * 利用ContentProvider扫描手机中所有的图片
     */
    private void initDatas() {

        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "SD卡不可用", Toast.LENGTH_SHORT);
            return;
        }

        mProgressDialog = ProgressDialog.show(this, "", "正在加载...");
        new Thread() {
            @Override
            public void run() {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + "= ? or " + MediaStore
                        .Images.Media
                        .MIME_TYPE + "= ?", new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media
                        .DATE_MODIFIED);
                Set<String> mDirPath = new HashSet<String>(); // 为了避免重复扫描
                while (cursor.moveToNext()) {
                    // 得到图片路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null)
                        continue;
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;
                    if (mDirPath.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPath.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }
                    if (parentFile.list() == null)
                        continue;
                    int picSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String filename) {
                            if (filename.endsWith(".jpg") || filename.endsWith("jpeg") || filename.endsWith(".png"))
                                return true;
                            return false;
                        }
                    }).length;
                    folderBean.setCount(picSize);
                    mFolderBeans.add(folderBean);
                    if (picSize > mMaxCount) { // 默认显示文件最多的
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close();
                mHandler.sendEmptyMessage(FINISH_LOAD);
            }
        }.start();
    }

    private void initView() {
        gv = (GridView) findViewById(R.id.gv);
        rl_bottom = (RelativeLayout) findViewById(R.id.rl_bottom);
        tv_dir_name = (TextView) findViewById(R.id.tv_dir_name);
        tv_dir_count = (TextView) findViewById(R.id.tv_dir_count);
    }



}
