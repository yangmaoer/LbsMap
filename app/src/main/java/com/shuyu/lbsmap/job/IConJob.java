package com.shuyu.lbsmap.job;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.os.Handler;

import com.baidu.mapapi.clusterutil.clustering.view.DefaultClusterRenderer;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import com.shuyu.lbsmap.DemoApplication;
import com.shuyu.lbsmap.event.IconEvent;
import com.shuyu.lbsmap.model.IconModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.finalteam.okhttpfinal.FileDownloadCallback;
import cn.finalteam.okhttpfinal.HttpRequest;

import static com.shuyu.lbsmap.utils.CommonUtil.getBitmapSize;
import static com.shuyu.lbsmap.utils.FileUtils.getLogoNamePath;

public class IConJob extends Job {

    private final static String TAG = "IConJob";

    private List<IconModel> logoUrlList = new ArrayList<>();
    private Handler handler;
    private int size = 0;

    protected IConJob() {
        super(new Params(1000));
    }

    public IConJob(List<IconModel> logoUrlList) {
        super(new Params(1000));
        this.logoUrlList = logoUrlList;
        handler = new Handler();

    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (logoUrlList != null && logoUrlList.size() > 0) {
                    DefaultClusterRenderer.LOADING_LOGO = true;
                    downloadFile(logoUrlList.get(size));
                }
            }
        });
    }

    @Override
    protected void onCancel() {

    }


    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        return false;
    }

    public void downloadFile(final IconModel iconModel) {
        final String name = getLogoNamePath(iconModel.getUrl()) + "tmp";
        File saveFile = new File(name);
        HttpRequest.download(iconModel.getUrl(), saveFile, new FileDownloadCallback() {
            @Override
            public void onDone() {
                super.onDone();
                Point point = getBitmapSize();
                transImage(name, name.replace("tmp", ""), point.x, point.y);

                IconEvent iconEvent = new IconEvent(IconEvent.EventType.success);
                iconEvent.seteId(iconModel.getId());
                DemoApplication.getApplication().getEventBus().post(iconEvent);

                resolveDownLoad();
            }

            @Override
            public void onFailure() {
                super.onFailure();
                File file = new File(name);
                if (file.exists()) {
                    file.delete();
                }
                resolveDownLoad();
            }
        });
    }

    private void resolveDownLoad() {
        size += 1;
        if (size >= (logoUrlList.size() - 1)) {
            DefaultClusterRenderer.LOADING_LOGO = false;
            IconEvent iconEvent = new IconEvent(IconEvent.EventType.success);
            DemoApplication.getApplication().getEventBus().post(iconEvent);
        } else {
            String url = logoUrlList.get(size).getUrl();
            final String name = getLogoNamePath(url);
            if (!new File(name).exists()) {
                downloadFile(logoUrlList.get(size));
            } else {
                resolveDownLoad();
            }
        }
    }

    public void transImage(String fromFile, String toFile, int width, int height) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(fromFile);
            if (bitmap == null) {
                File file = new File(fromFile);
                if (file.exists()) {
                    file.delete();
                }
                return;
            }
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();
            // 缩放图片的尺寸
            float scaleWidth = (float) width / bitmapWidth;
            float scaleHeight = (float) height / bitmapHeight;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            // 产生缩放后的Bitmap对象
            Bitmap resizeBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true);
            // save file
            File myCaptureFile = new File(toFile);
            FileOutputStream out = new FileOutputStream(myCaptureFile);
            if (resizeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
            if (!bitmap.isRecycled()) {
                bitmap.recycle();//记得释放资源，否则会内存溢出
            }
            if (!resizeBitmap.isRecycled()) {
                resizeBitmap.recycle();
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}