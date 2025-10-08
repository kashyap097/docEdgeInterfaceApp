package com.example.docedgeinterfaceapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class IntentActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 100;
    private static TextView filenameView;
    private static TextView fileSizeView;
    private static TextView filePathView;

    private static final String WSDL_URL = "https://docedge.pericent.com/services/Document?wsdl";

    // Namespace
    private static final String NAMESPACE = "http://document.webservice.docedge.com/";

    // Method name
    private static final String METHOD_NAME = "upload";

    // SOAP Action
    private static final String SOAP_ACTION = "http://document.webservice.docedge.com/DocumentService/upload";

    private static String sid = "";

    private static String filename = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intent);
        filenameView = findViewById(R.id.filename);
        fileSizeView = findViewById(R.id.filesize);
        receiveImage(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveImage(intent);
    }

    private void receiveImage(Intent intent) {
        System.out.println("intent activity +++++++++++++++++++");
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                try {
                    readImageContent(imageUri);
                    String fileInfo = getFileInfo(imageUri);
                } catch (Exception e) {
                    Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint("Range")
    private String getFileInfo(Uri uri) {
        String fileName = "";
        long fileSize = 0;
        String filePath = "";

        try {
            // Get file name and size
            String scheme = uri.getScheme();
            if (scheme != null && scheme.equals("content")) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                        fileSize = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
                        filePath = cursor.getString(0);
                    }
                }
            } else if (scheme != null && scheme.equals("file")) {
                File file = new File(uri.getPath());
                fileName = file.getName();
                fileSize = file.length();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        filename = fileName;
        sid = getIntent().getStringExtra("sid");
        filenameView.setText(fileName);
        fileSizeView.setText(String.valueOf(fileSize));
        //filePathView.setText(filePath);
        return "File Name: " + fileName + "\n" +
                "File Size: " + fileSize + " bytes" + "\n" + "File Path  : "+filePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri imageUri = data.getData();
                if (imageUri != null) {
                    try {
                        // Display image
                        //imageView.setImageURI(imageUri);
                        String fileInfo = getFileInfo(imageUri);

                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void onImageUriReceived(Uri imageUri) {
        readImageContent(imageUri);
    }
    private void readImageContent(Uri imageUri) {
        new ImageContentReader(this).execute(imageUri);
    }

    private static class ImageContentReader extends AsyncTask<Uri, Void, String> {
        private static final String TAG = "ImageContentReader";
        private Context context;

        public ImageContentReader(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return readContentFromImageUri(context, imageUri);
        }

        private String readContentFromImageUri(Context context, Uri imageUri) {
            byte[] contentBytes = null;
            ContentResolver contentResolver = context.getContentResolver();
            try (InputStream inputStream = contentResolver.openInputStream(imageUri);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                contentBytes = outputStream.toByteArray();
            } catch (IOException e) {
                Log.e(TAG, "Error reading content from image URI", e);
            }
            if (contentBytes != null) {
                return Base64.encodeToString(contentBytes, Base64.DEFAULT);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String content) {

              String filecontent = content;
            new IntentActivity.SOAPRequestTask().execute(sid, filename, filecontent);
//            AlertDialog.Builder builder = new AlertDialog.Builder(context);
//            builder.setMessage(content)
//                    .setTitle("Image Content (Base64)")
//                    .setPositiveButton("OK", null)
//                    .create()
//                    .show();
//
//            Log.d(TAG, "Image content: " + content);



        }
    }


    private static class SOAPRequestTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... params) {
            try {
                // Create SOAP request
                String sid = params[0];
                String filename = params[1];
                String filecontent = params[2];
                SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                PropertyInfo usernameInfo = new PropertyInfo();
                usernameInfo.setName("sid");
                usernameInfo.setValue(sid);
                usernameInfo.setType(String.class);
                request.addProperty(usernameInfo);

                PropertyInfo folderInfo = new PropertyInfo();
                folderInfo.setName("folderId");
                folderInfo.setValue(98440959);
                folderInfo.setType(String.class);
                request.addProperty(folderInfo);

                PropertyInfo passwordInfo = new PropertyInfo();
                passwordInfo.setName("filename");
                passwordInfo.setValue(filename);
                passwordInfo.setType(String.class);
                request.addProperty(passwordInfo);

                PropertyInfo contentInfo = new PropertyInfo();
                contentInfo.setName("content");
                contentInfo.setValue(filecontent);
                contentInfo.setType(String.class);
                request.addProperty(contentInfo);
                // Setup SOAP envelope
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.setOutputSoapObject(request);

                // Setup HTTP transport
                HttpTransportSE httpTransport = new HttpTransportSE(WSDL_URL);

                // Call the web service
                httpTransport.call(SOAP_ACTION, envelope);

                // Get the response
                Object response = envelope.getResponse();
                // Check if the response is a SoapPrimitive
                if (response instanceof SoapPrimitive) {
                    return response.toString();
                } else {
                    // Handle other types of response or unexpected scenarios
                    return "Unexpected response format";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {

            //Toast.makeText(IntentActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
            //System.out.println("======result====="+result);

        }
    }
}
