package com.punkrockdigital.barcodescanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //UI Elements:
    int mColor = Color.rgb(148,255,98); // Edit color here!
    TextView txtPrice;
    TextView txtBarNumber;
    TextView txtTitle;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Switch swtCamera;
    Surface surface;

    //Camera Elements:
    CameraSource cameraSource;
    CameraThing cameraThing;
    int CAMERA_PERMISSION = 200;

    //Scanner Elements:
    BarcodeDetector detector;

    //Random S#it:
    Vibrator vibrator;
    String usedPrice;
    String title;
    String author;
    String edition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setUI();    //Sets UI elements and certain features. Set the text color from this Method!
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        setUp();
    }

    @Override
    protected void onPause(){
        super.onPause();
        cameraSource.stop();
        detector.release();
        cameraSource.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUp();
    }

    //Failed attempt to turn off the camera programmatically.
    private void setOnChange() {
        swtCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!isChecked)
                    takeDown();
                else
                    setUp();
            }
        });
    }

    private void takeDown() {
        cameraSource.release();
        swtCamera.setTextColor(Color.RED);
    }

    public void setUp() {
        detector = new BarcodeDetector.Builder(getApplicationContext()).setBarcodeFormats(Barcode.EAN_13).build();
        setDetector();

        txtBarNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detector.release();
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        setDetector();
                        vibrator.vibrate(50);

                    }
                }, 1000);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        setCamera();    //Sets the camera up for Barcode grabs
        swtCamera.setTextColor(Color.BLUE);
    }
    private String convertISToString(InputStream is)
    throws IOException{
        if(is != null){
            StringBuilder sb = new StringBuilder();
            String line;
            try{
                BufferedReader r1 = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while ((line = r1.readLine()) != null){
                    sb.append(line);
                }
            }
            finally {
                is.close();
            }
            return sb.toString();
        }
        else {
            return "";
        }
    }
    private void setDetector() {
        detector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {


            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                txtBarNumber.setText(detections.getDetectedItems().valueAt(0).displayValue);
                //URL Connection:
                String parsedString = "";
                try{
                    URL url = new URL("http://www.fastbuyback.com/ep1.php?isbn=" + txtBarNumber.getText());
                    URLConnection conn = url.openConnection();
                    HttpURLConnection httpConn = (HttpURLConnection) conn;
                    httpConn.setAllowUserInteraction(false);
                    httpConn.setInstanceFollowRedirects(true);
                    httpConn.setRequestMethod("GET");
                    httpConn.connect();
                    InputStream is = httpConn.getInputStream();
                    parsedString = convertISToString(is);

                    txtPrice.setText("");
                    txtTitle.setText("");

                    if (parsedString.equals("null"))
                        txtTitle.setText("Null data, or bad \n Barcode read!");

                    if(parsedString != "null"){
                        JSONArray array = new JSONArray(parsedString);
                        array.isNull(0);
                        JSONObject object = array.getJSONObject(0);
                        author = "By: " + object.getString("author");
                        usedPrice = "$" + object.getString("usedPrice");
                        title = object.getString("title");
                        edition = object.getString("edition") + " Edition";
                        txtPrice.setText(usedPrice);
                        txtPrice.setBackgroundColor(Color.argb(120, 0,0,0));
                        txtTitle.setBackgroundColor(Color.argb(120, 0,0,0));
                        String s = title + " \n " + author + " \n " + edition;
                        txtTitle.setText(s);
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void setCamera() {
        cameraThing = new CameraThing();
        surfaceHolder = surfaceView.getHolder();
        surface = surfaceHolder.getSurface();
        cameraThing.askPermission(getApplicationContext(), MainActivity.this, CAMERA_PERMISSION);
        cameraThing.setPreviewSize(getWindowManager());
        cameraSource = cameraThing.setCameraSource(getApplicationContext(), detector);
        cameraThing.setSurfaceCallback(surfaceHolder, getApplicationContext(), cameraSource, MainActivity.this, CAMERA_PERMISSION);
    }

    private void setUI() {
        txtPrice = findViewById(R.id.txtPrice);
        txtBarNumber = findViewById(R.id.txtBarNumber);
        txtTitle = findViewById(R.id.txtTitle);
        surfaceView = findViewById(R.id.surfaceView);
        swtCamera = findViewById(R.id.swtCamera);

        Typeface face = Typeface.createFromAsset(getAssets(),"fonts/ARIALBD.TTF");
        Typeface mface = Typeface.createFromAsset(getAssets(),"fonts/Arial.ttf");
        txtPrice.setTypeface(face);
        txtTitle.setTypeface(mface);
        txtTitle.setSingleLine(false);

        txtPrice.setTextColor(mColor);
        txtTitle.setTextColor(mColor);

        vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
    }
}