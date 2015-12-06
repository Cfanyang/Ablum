package com.cola.code.album.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.cola.code.album.R;
import com.cola.code.album.support.ImgLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by yanghaoyang on 15/11/5.
 */
public class ImageAdapter extends BaseAdapter {
    private static Set<String> mSelectedImg = new HashSet<String>();
    private Context mContext;
    private List<String> mImgName;
    private String mDirPath;

    public ImageAdapter(Context context, List<String> mDatas, String dirPath) {
        this.mContext = context;
        this.mImgName = mDatas;
        this.mDirPath = dirPath;
    }

    @Override
    public int getCount() {
        return mImgName.size();
    }

    @Override
    public Object getItem(int position) {
        return mImgName.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.item_pic, null);
            viewHolder = new ViewHolder();
            viewHolder.iv_item = (ImageView) convertView.findViewById(R.id.iv_item);
            viewHolder.ib_check = (ImageButton) convertView.findViewById(R.id.ib_check);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        // 重置状态
        viewHolder.iv_item.setImageResource(R.drawable.shape_default_pic);
        viewHolder.ib_check.setImageResource(R.drawable.tb_circle);
        viewHolder.iv_item.setColorFilter(null);

        ImgLoader.getInstance(3, ImgLoader.Type.FILO).loadImg(mDirPath + "/" + mImgName.get(position), viewHolder
                .iv_item);
        final String filePath = mDirPath + "/" + mImgName.get(position);
        // TODO 尝试优化
        viewHolder.iv_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedImg.contains(filePath)) { // 如果已选中
                    mSelectedImg.remove(filePath);
                    viewHolder.iv_item.setColorFilter(null);
                    viewHolder.ib_check.setImageResource(R.drawable.tb_circle);
                } else { // 如果没有选中
                    mSelectedImg.add(filePath);
                    viewHolder.iv_item.setColorFilter(Color.parseColor("#77000000"));
                    viewHolder.ib_check.setImageResource(R.drawable.tb_checkcircle);
                }
            }
        });
        if (mSelectedImg.contains(filePath)) {
            viewHolder.iv_item.setColorFilter(Color.parseColor("#77000000"));
            viewHolder.ib_check.setImageResource(R.drawable.tb_checkcircle);
        }
        return convertView;
    }

    private class ViewHolder {
        public ImageView iv_item;
        public ImageButton ib_check;
    }
}
