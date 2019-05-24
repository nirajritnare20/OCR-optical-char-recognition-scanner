package com.example.smartscan;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/Tess";
    private static final String SAVE_PATH ="/savedText";
    private TextView textView;
    private TessBaseAPI tessBaseAPI;
    private Uri outputFileDir, imgUri;
    public static final int PICK_IMAGE = 200;
    public static final int CLICK_IMAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.textView);

        this.findViewById(R.id.cameraButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraActivity();
            }
        });


        ImageButton secondActButtn = (ImageButton) findViewById(R.id.filesButton);
        secondActButtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                openGalleryActivity();

            }
        });

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    private void openGalleryActivity(){
        try {
            final Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            if (gallery.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(gallery, PICK_IMAGE);
            }
        }
        catch (Exception e){
            Log.e(TAG, e.getMessage());
        }

    }

    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


    private void startCameraActivity() {
        try{
            String imagePath = DATA_PATH + "/imgs";
            File dir = new File(imagePath);
            if(!dir.exists()){
                dir.mkdir();
            }
            String imageFilePath = imagePath+"/ocr.jpg";
            outputFileDir = Uri.fromFile(new File(imageFilePath));
            final Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileDir);
            if(pictureIntent.resolveActivity(getPackageManager()) != null){
                startActivityForResult(pictureIntent, CLICK_IMAGE);
            }
        }
        catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CLICK_IMAGE && resultCode == RESULT_OK) {
                Toast.makeText(getApplicationContext(), "called",Toast.LENGTH_SHORT).show();
                prepareTessData();
                Log.d("value of outputFile is ", String.valueOf(outputFileDir));
                startOCR(outputFileDir);

            }
            else if(requestCode == PICK_IMAGE && resultCode == RESULT_OK){
               /* super.onActivityResult(requestCode, resultCode, data);
                imgUri = data.getData();
                Toast.makeText(getApplicationContext(), "called",Toast.LENGTH_SHORT).show();
                Log.d("value of img is ", String.valueOf(imgUri));
                prepareTessData();
                startOCR(imgUri);*/
                imgUri = data.getData();
                String imagePath = getRealPathFromURI(imgUri);
                Log.d("value of imageFile is", String.valueOf(imagePath));
                imgUri = Uri.fromFile(new File(imagePath));
                prepareTessData();
                startOCR(imgUri);

        }
            else {
                Toast.makeText(getApplicationContext(), "image problem.", Toast.LENGTH_SHORT).show();
            }
        }


    private void prepareTessData(){
        try{
            File dir= new File(DATA_PATH + TESS_DATA);
            if(!dir.exists()){
                dir.mkdir();
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = DATA_PATH + TESS_DATA+ "/" + fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void startOCR(Uri imageUri){
        try{
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 6;
            Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(), options);
            String result = this.getText(bitmap);
            textView.setText(result);
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private String getText(Bitmap bitmap){
        try{
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.init(DATA_PATH,"eng");
        tessBaseAPI.setImage(bitmap);
        String retStr = "No result";
        try{
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        tessBaseAPI.end();
        SaveTextAsFile(retStr);
        return retStr;
    }

    private void SaveTextAsFile(String content) {
        try {
            File dir= new File(DATA_PATH + SAVE_PATH);
            if(!dir.exists()){
                dir.mkdir();
            }
            File file = new File(DATA_PATH + SAVE_PATH + "/OCR.txt");
            Log.d("value of file is ", String.valueOf(file));
            FileOutputStream fos = new FileOutputStream(file, true);
            //content = content +"\n\n";
            //fos.write(content.getBytes());

            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.append(content+"\n\n________________________________________\n\n");
            Toast.makeText(getApplicationContext(), "saved successfully.", Toast.LENGTH_SHORT).show();
            writer.close();
            fos.close();



            //fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "File not found.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error saving!", Toast.LENGTH_SHORT).show();
        }
    }
}
