package com.cola.code.album.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.cola.code.album.R;
import com.cola.code.album.bean.FolderBean;
import com.cola.code.album.support.ImgLoader;

import java.util.List;

/**
 * Created by yanghaoyang on 15/11/5.
 */
public class ListImgDirPopupWindow extends PopupWindow{
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mDatas;
    private OnDirSelectedListener mListener;

    public interface OnDirSelectedListener {
        public void onSelected(FolderBean folderBean);
    }

    public ListImgDirPopupWindow(Context context,List<FolderBean> datas) {
        calWidthANDHeight(context);
        mConvertView = View.inflate(context, R.layout.popup_choose_dir,null);
        mDatas = datas;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true );
        setOutsideTouchable(true );
        setBackgroundDrawable(new BitmapDrawable());
        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                 if(event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                     dismiss();
                     return true;
                 }
                return false;
            }
        });

        initView(context);
        initEvent();
    }

    public void setOnDirSelectedListener(OnDirSelectedListener listener) {
        this.mListener = listener;
    }

    private void initEvent() {

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mListener != null) {
                    mListener.onSelected(mDatas.get(position));
                }
            }
        });

    }

    private void initView(Context context) {
        mListView = (ListView) mConvertView.findViewById(R.id.lv_dir);
        mListView.setAdapter(new ListDirAdapter(context,mDatas));
    }

    private void calWidthANDHeight(Context context ) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        mWidth = metrics.widthPixels;
        mHeight = (int) (metrics.heightPixels * 0.7);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean> {

        private Context context;

         public ListDirAdapter(Context context,List<FolderBean> list) {
            super(context,0,list);
             this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if(convertView == null) {
                convertView = View.inflate(context,R.layout.item_popup_choose_dir,null);
                viewHolder = new ViewHolder();
                viewHolder.iv_dir = (ImageView) convertView.findViewById(R.id.iv_dir);
                viewHolder.tv_dri_name = (TextView) convertView.findViewById(R.id.tv_dir_name);
                viewHolder.tv_count = (TextView) convertView.findViewById(R.id.tv_count);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.iv_dir.setImageResource(R.drawable.shape_default_pic);
            FolderBean bean = getItem(position);
            ImgLoader.getInstance(3, ImgLoader.Type.FILO).loadImg(bean.getFirstImgPath(),viewHolder.iv_dir);
            viewHolder.tv_dri_name.setText(bean.getName());
            viewHolder.tv_count.setText(bean.getCount() + "");
            return convertView;
        }

        private class ViewHolder {
            public ImageView iv_dir;
            public TextView tv_dri_name,tv_count;
        }
    }

}
