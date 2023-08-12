package com.example.textdetector;

import static android.Manifest.permission.CAMERA;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.icu.util.Calendar;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class ScannerActivity extends AppCompatActivity {
    private ImageView captureIV;
    private ImageView captureIV2;
    private TextView resultTV;
    private Button snapBtn, detectBtn, snappyBtn, saveImageBtn;
    private Bitmap imageBitmap, imageBitmap2;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_CAPTURE_2 = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        captureIV = findViewById(R.id.IVCaptureImage);
        captureIV2 = findViewById(R.id.IVCaptureImage2);
        resultTV = findViewById(R.id.idTVDetectedText);
        snapBtn = findViewById(R.id.idBtnSnap);
        detectBtn = findViewById(R.id.idBtnDetect);
        snappyBtn = findViewById(R.id.idBtnSnappy);
        saveImageBtn = findViewById(R.id.idBtnSaveImage);

        saveImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    saveImageWithText();
                } else {
                    requestPermission();
                }
            }
        });
        detectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageBitmap != null) {
                    scanQRCode();
                } else {
                    Toast.makeText(ScannerActivity.this, "Please capture the first image before scanning QR code.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        snapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    captureImage();
                } else {
                    requestPermission();
                }
            }
        });
        snappyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    captureImage2();
                } else {
                    requestPermission();
                }
            }
        });
    }
    private String detectedText;
    private boolean checkPermissions(){
        int cameraPermission= ContextCompat.checkSelfPermission(getApplicationContext(),CAMERA);
        return cameraPermission== PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission(){
        int PERMISSION_CODE=200;
        ActivityCompat.requestPermissions(this,new String[]{CAMERA},PERMISSION_CODE);
    }

    private void captureImage(){
        Intent takePicture=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture,REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0){
            boolean cameraPermission = grantResults[0] ==  PackageManager.PERMISSION_GRANTED;
            if(cameraPermission){
                Toast.makeText(this,"Permissions granted..",Toast.LENGTH_SHORT).show();
                captureImage();
            }else{
                Toast.makeText(this,"Permissions denied..",Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void captureImage2() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePicture, REQUEST_IMAGE_CAPTURE_2);
        }
    }
    private void scanQRCode() {
        if (imageBitmap != null) {
            InputImage image = InputImage.fromBitmap(imageBitmap, 0);

            // Configure the QR code scanner options
            BarcodeScannerOptions options =
                    new BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build();

            // Create the barcode scanner
            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            // Process the image for QR code scanning
            Task<List<Barcode>> result = scanner.process(image).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                @Override
                public void onSuccess(List<Barcode> barcodes) {
                    processQRCodeResult(barcodes); // Process the scanned QR codes
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ScannerActivity.this, "Failed to scan QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    private void processQRCodeResult(List<Barcode> barcodes) {
        StringBuilder resultText = new StringBuilder();
        for (Barcode barcode : barcodes) {
            resultText.append("QR Code Value: ").append(barcode.getRawValue()).append("\n");

            // Log the detected QR code value
            Log.d("QR_CODE", "Detected QR code: " + barcode.getRawValue());

            // Pass the detected QR code value to overlayTextOnImage
            overlayTextOnImage(imageBitmap2, barcode.getRawValue());

            // Set the detected QR code result on the resultTV
            resultTV.setText(resultText.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            captureIV.setImageBitmap(imageBitmap);
            //detectText(); // Detect text from the first image
        } else if (requestCode == REQUEST_IMAGE_CAPTURE_2 && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap2 = (Bitmap) extras.get("data");
            captureIV2.setImageBitmap(imageBitmap2);

            // Call detectTextOnImage2 with the detected text from the first image
            if (detectedText != null && !detectedText.isEmpty()) {
                detectTextOnImage2(detectedText);
            }
        }
    }
    /*private void detectText() {
        if (imageBitmap != null) {
            InputImage image = InputImage.fromBitmap(imageBitmap, 0);
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            Task<Text> result = recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>() {
                @Override
                public void onSuccess(Text text) {
                    processTextResult(text); // Process the detected text and save it
                    detectTextOnImage2(detectedText); // Proceed to detect text on the second image and overlay
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(ScannerActivity.this, "Failed to detect text: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }*/
    private void detectTextOnImage2(final String qrCodeValue) {
        if (imageBitmap2 != null) {
            // Overlay the detected QR code value from the first image onto the second image
            overlayTextOnImage(imageBitmap2, qrCodeValue);

            // Display the second image with overlaid text
            captureIV2.setImageBitmap(imageBitmap2);
            captureIV2.setVisibility(View.VISIBLE); // Make the second image view visible
        } else {
            Toast.makeText(ScannerActivity.this, "Please capture the second image before detecting text.", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveImageWithText() {
        if (imageBitmap2 != null) {
            Bitmap bitmapWithText = imageBitmap2.copy(Bitmap.Config.ARGB_8888, true);

            // Overlay the detected text on the image
            String qrCodeValue = "Your QR Code Value Here"; // Replace with your actual QR code value
            overlayTextOnImage(bitmapWithText, qrCodeValue);

            try {
                String dateTime = getFormattedDateTime();
                String fileName = "ImageWithText_" + dateTime + ".jpg";

                File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "TIEI_DTA");
                if (!directory.exists()) {
                    if (!directory.mkdirs()) {
                        Log.e("SaveImage", "Failed to create directory");
                        return;
                    }
                }

                Log.d("SaveImage", "Directory: " + directory.getAbsolutePath());

                File file = new File(directory, fileName);
                Log.d("SaveImage", "File path: " + file.getAbsolutePath());

                FileOutputStream fos = new FileOutputStream(file);
                bitmapWithText.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
                fos.close();

                // Notify the MediaScanner to add the saved image to the gallery
                MediaScannerConnection.scanFile(this, new String[]{file.getPath()}, null, null);

                Toast.makeText(this, "Image saved successfully.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("SaveImage", "Failed to save image: " + e.getMessage());
                Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No image to save.", Toast.LENGTH_SHORT).show();
        }
    }
    private void overlayTextOnImage(Bitmap image, String qrCodeValue){
        if (image != null && qrCodeValue != null && !qrCodeValue.isEmpty()) {
            // Create a copy of the image to avoid modifying the original image
            Bitmap bitmapWithText = image.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(bitmapWithText);
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setTextSize(10);

            // Adjust the position to overlay the text
            float x = 0;
            float y = 50;

            // Split the QR code value, date, and time with underscore
            String dateTime = getFormattedDateTime(); // Get formatted date and time
            String[] lines = (qrCodeValue + "_" + dateTime).split("_");

            for (String line : lines) {
                canvas.drawText(line, x, y, paint);
                y += 10; // Adjust the vertical spacing between lines
            }
            Log.d("Overlay", "Overlaying text on image: " + qrCodeValue);
            Log.d("Overlay", "Image dimensions: " + image.getWidth() + " x " + image.getHeight());
            Log.d("Overlay", "Coordinates (x, y): " + x + ", " + y);
            captureIV2.setImageBitmap(bitmapWithText);
            captureIV2.setVisibility(View.VISIBLE); // Make the second image view visible
        }
    }

    @SuppressLint("DefaultLocale")
    private String getFormattedDateTime() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%04d-%02d-%02d_%02d:%02d:%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                calendar.get(Calendar.SECOND));
    }
    private void processTextResult(Text text) {
        StringBuilder resultText = new StringBuilder();
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                for (Text.Element element : line.getElements()) {
                    resultText.append(element.getText()).append(" ");
                }
            }
        }
        // Set the detected text on the resultTV
        resultTV.setText(resultText.toString());
        // Save the detected text to the 'detectedText' variable
        detectedText = resultText.toString();
    }
}