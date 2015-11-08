package com.cola.code.album.bean;

/**
 * Created by yanghaoyang on 15/10/31.
 */
public class FolderBean {
    /**
     * 当前文件路径
     */
    private String dir;
    /**
     * 第一张图片的路径
     */
    private String firstImgPath;
    /**
     * 当前文件的名字
     */
    private String name;
    /**
     * 当前文件的图片数量
     */
    private int count;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = dir.lastIndexOf("/");
        this.name = dir.substring(lastIndexOf);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }
}
