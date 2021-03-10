package io.flutter.plugins.webviewflutter;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileUploadActivity extends Activity {

    public static final int CHOOSE_FILE_REQUEST_CODE = 1;
    private static ValueCallback<Uri[]> mUploadMessageArray;
    private Uri fileUri;
    private Uri videoUri;
    private static WebChromeClient.FileChooserParams mFileChooserParams;

    public static final String IMAGE_EXTRA = "IMAGE_EXTRA";
    public static final String VIDEO_EXTRA = "VIDEO_EXTRA";

    public static void getFilePathCallback(ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        mUploadMessageArray = filePathCallback;
        mFileChooserParams = fileChooserParams;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isAcceptImage = getIntent().getBooleanExtra(IMAGE_EXTRA, false);
        boolean isAcceptVideo = getIntent().getBooleanExtra(VIDEO_EXTRA, false);

        /// Call FileChooser
        showFileChooser(isAcceptImage, isAcceptVideo);
    }

    public void showFileChooser(boolean isAcceptImage, boolean isAcceptVideo) {
        List<Intent> intentList = new ArrayList<Intent>();
        if (isAcceptImage) {
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            fileUri = getOutputFilename(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            intentList.add(takePhotoIntent);
        }

        if (isAcceptVideo) {
            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            videoUri = getOutputFilename(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
            intentList.add(takeVideoIntent);
        }

        Intent contentSelectionIntent;
        if (Build.VERSION.SDK_INT >= 21 && mFileChooserParams != null) {
            final boolean allowMultiple = mFileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE;
            contentSelectionIntent = mFileChooserParams.createIntent();
            contentSelectionIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            contentSelectionIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            contentSelectionIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        } else {
            contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");
        }

        Intent[] intentArray = intentList.toArray(new Intent[intentList.size()]);

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        startActivityForResult(chooserIntent, CHOOSE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= 21) {
            if (requestCode == CHOOSE_FILE_REQUEST_CODE) {
                Uri[] results = null;
                if (resultCode == Activity.RESULT_OK) {
                    if (fileUri != null && getFileSize(fileUri) > 0) {
                        results = new Uri[]{fileUri};
                    } else if (videoUri != null && getFileSize(videoUri) > 0) {
                        results = new Uri[]{videoUri};
                    } else if (getApplicationContext() != null) {
                        results = getSelectedFiles(data);
                    }
                }

                if (mUploadMessageArray != null) {
                    mUploadMessageArray.onReceiveValue(results);
                }
            }
        } else {
            mUploadMessageArray.onReceiveValue(null);
        }
        finish();
    }

    private Uri getOutputFilename(String intentType) {
        String prefix = "";
        String suffix = "";

        if (intentType == MediaStore.ACTION_IMAGE_CAPTURE) {
            prefix = "image-";
            suffix = ".jpg";
        } else if (intentType == MediaStore.ACTION_VIDEO_CAPTURE) {
            prefix = "video-";
            suffix = ".mp4";
        }

        String packageName = getApplicationContext().getPackageName();
        File capturedFile = null;
        try {
            capturedFile = createCapturedFile(prefix, suffix);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return FileProvider.getUriForFile(getApplicationContext(), packageName + ".fileprovider", capturedFile);
    }

    private File createCapturedFile(String prefix, String suffix) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = prefix + "_" + timeStamp;
        File storageDir = getApplicationContext().getExternalFilesDir(null);
        return File.createTempFile(imageFileName, suffix, storageDir);
    }

    private long getFileSize(Uri fileUri) {
        Cursor returnCursor = getApplicationContext().getContentResolver().query(fileUri, null, null, null, null);
        returnCursor.moveToFirst();
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        return returnCursor.getLong(sizeIndex);
    }

    private Uri[] getSelectedFiles(Intent data) {
        // we have one files selected
        if (data.getData() != null) {
            String dataString = data.getDataString();
            if (dataString != null) {
                return new Uri[]{Uri.parse(dataString)};
            }
        }
        // we have multiple files selected
        if (data.getClipData() != null) {
            final int numSelectedFiles = data.getClipData().getItemCount();
            Uri[] result = new Uri[numSelectedFiles];
            for (int i = 0; i < numSelectedFiles; i++) {
                result[i] = data.getClipData().getItemAt(i).getUri();
            }
            return result;
        }
        return null;
    }
}
