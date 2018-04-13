package eu.neosurance.sdk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;

import id.zelory.compressor.Compressor;

public class NSRBase64Image {
    public static final int REQUEST_IMAGE_CAPTURE = 0x1702;
    public static final String FILENAME_IMAGE_CAPTURE = "nsr-photo.jpg";
    private static NSRBase64Image instance;
    private Activity activity;

    private  NSRBase64Image(Context context) {
        this.activity = (Activity) context;
    }

    public static NSRBase64Image getInstance(Context context){
        if(instance == null){
            instance = new NSRBase64Image(context);
        }else{
            instance.activity = (Activity) context;
        }
        return instance;
    }

    public File getFileTemp(){
        final File path = new File(Environment.getExternalStorageDirectory(), activity.getPackageName());
        if(!path.exists()){
            path.mkdir();
        }
        return new File(path, FILENAME_IMAGE_CAPTURE);
    }

    public void takePhoto(){
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (mIntent.resolveActivity(activity.getPackageManager()) != null) {
                mIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity, activity.getPackageName()+".provider", getFileTemp()));
                activity.startActivityForResult(mIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public void registerCallback(NSRCallbackManager callbackManager, final NSRBase64Image.Callback callback){
        if(!(callbackManager instanceof  NSRCallbackManagerImpl)){
            throw new NullPointerException("callbackManager");
        }
        ((NSRCallbackManagerImpl) callbackManager).registerCallback(REQUEST_IMAGE_CAPTURE, new NSRCallbackManagerImpl.Callback() {
            public boolean onActivityResult(int resultCode, Intent data) {
                if(resultCode == Activity.RESULT_OK){
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                String imageProcessed = activity.getCacheDir().getAbsolutePath()+"/"+FILENAME_IMAGE_CAPTURE;
                                new Compressor(activity)
                                        .setMaxWidth(512)
                                        .setMaxHeight(512)
                                        .setQuality(60)
                                        .setCompressFormat(Bitmap.CompressFormat.JPEG)
                                        .setDestinationDirectoryPath(activity.getCacheDir().getAbsolutePath())
                                        .compressToFile(getFileTemp());

                                BitmapFactory.Options options = new BitmapFactory.Options();
                                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                                Bitmap bitmap = BitmapFactory.decodeFile(imageProcessed, options);;
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

                                final String imageBase64 = "data:image/jpeg;base64,"+encodedImage;
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        callback.onSuccess(imageBase64);
                                    }
                                });

                            } catch (Exception e) {
                                callback.onError();
                            }
                        }
                    }).start();
                }
                if(resultCode == Activity.RESULT_CANCELED){
                    callback.onCancel();
                }
                return true;
            }
        });
    }

    public interface Callback {
        public void onSuccess(String base64Image);
        public void onCancel();
        public void onError();
    }

}


