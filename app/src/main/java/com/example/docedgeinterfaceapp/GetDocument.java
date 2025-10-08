package com.example.docedgeinterfaceapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;


public class GetDocument extends AppCompatActivity {

    private static final int REQUEST_CODE = 100;
    private static TextView filenameView;
    private static TextView fileSizeView;

    private static TextView fileTypeView;
    private String url = "";
    private Button buttonUpload;
    private ImageButton buttonMenu;
    private static String WSDL_URL = "";
    private static final String NAMESPACE = "http://document.webservice.docedge.com/";

    // Method name
    private static final String METHOD_NAME = "upload";

    private static final String METHOD_NAME1 = "getExternalUploadingFolderId";

    // SOAP Action
    private static final String SOAP_ACTION = "http://document.webservice.docedge.com/DocumentService/upload";
    private static final String SOAP_ACTION1 = "http://document.webservice.docedge.com/DocumentService/getExternalUploadingFolderId";

    private static final String LogoutNAMESPACE = "http://auth.webservice.docedge.com/";

    // Method name
    private static final String Logout_METHOD_NAME = "logout";

    // SOAP Action
    private static final String Logout_SOAP_ACTION = "http://auth.webservice.docedge.com/AuthService/logout";

    private static String sid = "";

    private static String filename = "";

    private static String filecontent = "";
    private static String folderId = "";
    private static final int MENU_ITEM_1_ID = 1;

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String user_Sid = "sid";
    private ProgressBar spinner;

    SharedPreferences sharedpreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_intent);
            filenameView = findViewById(R.id.filename);
            fileSizeView = findViewById(R.id.filesize);
            fileTypeView = findViewById(R.id.filetype);
            buttonUpload = findViewById(R.id.buttonUpload);
            buttonMenu = findViewById(R.id.menu);
            spinner=(ProgressBar)findViewById(R.id.progressBar);
            spinner.setVisibility(View.GONE);
            receiveDocument(getIntent());


            buttonUpload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!sid.isEmpty() && !filename.isEmpty() && !filecontent.isEmpty()) {
                        spinner.setVisibility(View.VISIBLE);
                        new SOAPRequestTask().execute(sid, filename, filecontent);
                    } else {
                        Toast.makeText(GetDocument.this, "Please select a document", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            buttonMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // new logout().execute();
                    showPopupMenu(v);
                }
            });

        }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        receiveDocument(intent);
    }

    private void receiveDocument(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (imageUri != null) {
                try {
                    //readImageContent(imageUri);
                    onDocumentUriReceived(imageUri);
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
        url = getIntent().getStringExtra("url");
        WSDL_URL = url + "/services/Document?wsdl";
        filenameView.setText(fileName.substring(0,fileName.lastIndexOf(".")));
        fileSizeView.setText(String.valueOf(fileSize));
        fileTypeView.setText(fileName.substring(fileName.lastIndexOf(".")+1));
        //filePathView.setText(filePath);
        new SOAPRequestTask1().execute(sid);
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
                        String fileInfo = getFileInfo(imageUri);

                    } catch (Exception e) {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void onDocumentUriReceived(Uri documentUri) {
        readDocumentContent(documentUri);
    }

    private void readDocumentContent(Uri documentUri) {
        new DocumentContentReader(this).execute(documentUri);

    }

    // AsyncTask to read the content of the document in the background
    private class DocumentContentReader extends AsyncTask<Uri, Void, String> {
        private static final String TAG = "DocumentContentReader";
        private Context context;

        public DocumentContentReader(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(Uri... uris) {
            Uri documentUri = uris[0];
            return readContentFromDocumentUri(context, documentUri);
        }

        private String readContentFromDocumentUri(Context context, Uri documentUri) {
            byte[] contentBytes = null;
            ContentResolver contentResolver = context.getContentResolver();
            try (InputStream inputStream = contentResolver.openInputStream(documentUri);
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                contentBytes = outputStream.toByteArray();
            } catch (IOException e) {
                Log.e(TAG, "Error reading content from document URI", e);
            }
            if (contentBytes != null) {
                return Base64.encodeToString(contentBytes, Base64.DEFAULT);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String contentBase64) {
            if (contentBase64 != null) {
                filecontent = contentBase64;
                if (!sid.isEmpty() && !filename.isEmpty() && !filecontent.isEmpty()) {
                    spinner.setVisibility(View.VISIBLE);
                    new SOAPRequestTask().execute(sid, filename, filecontent);
                } else {
                    Toast.makeText(GetDocument.this, "Please select a document", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(GetDocument.this, "Document is not selected", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Example usage: Call this method with the document URI obtained from the intent
    private void handleIntent(Intent intent) {
        Uri documentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (documentUri != null) {
            onDocumentUriReceived(documentUri);
        }
    }

    private class SOAPRequestTask extends AsyncTask<String, Void, String>{

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
                System.out.println("sid kya hai : "+sid);
                usernameInfo.setValue(sid);
                usernameInfo.setType(String.class);
                request.addProperty(usernameInfo);

                PropertyInfo folderInfo = new PropertyInfo();
                folderInfo.setName("folderId");
                folderInfo.setValue(folderId);
                System.out.println("folder id : "+folderId);
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
            Toast.makeText(GetDocument.this, "Document successfully uploaded", Toast.LENGTH_SHORT).show();
                spinner.setVisibility(View.GONE);
            Intent intent = new Intent(GetDocument.this, WelcomeActivity.class);
            intent.putExtra("sid", getIntent().getStringExtra("sid"));
            intent.putExtra("url", getIntent().getStringExtra("url"));
            startActivity(intent);
        }
    }

    private class SOAPRequestTask1 extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                String sid = params[0];

                SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME1);
                PropertyInfo usernameInfo = new PropertyInfo();
                usernameInfo.setName("sid");
                usernameInfo.setValue(sid);
                usernameInfo.setType(String.class);
                request.addProperty(usernameInfo);

                // Setup SOAP envelope
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.setOutputSoapObject(request);

                // Setup HTTP transport
                HttpTransportSE httpTransport = new HttpTransportSE(WSDL_URL);

                // Call the web service
                httpTransport.call(SOAP_ACTION1, envelope);

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
            //Toast.makeText(GetDocument.this, "Folder ID" +result, Toast.LENGTH_SHORT).show();
            folderId = result;
//            spinner.setVisibility(View.GONE);
        }

    }


    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new logout().execute();
                        finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();

    }

    private class logout extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                SoapObject request = new SoapObject(LogoutNAMESPACE, Logout_METHOD_NAME);
                PropertyInfo usernameInfo = new PropertyInfo();
                usernameInfo.setName("sid");
                usernameInfo.setValue(sid);
                usernameInfo.setType(String.class);
                request.addProperty(usernameInfo);

                // Setup SOAP envelope
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.setOutputSoapObject(request);

                // Setup HTTP transport
                url = getIntent().getStringExtra("url");
                String logouturl = url + "/services/Auth?wsdl";
                HttpTransportSE httpTransport = new HttpTransportSE(logouturl);

                // Call the web service
                httpTransport.call(Logout_SOAP_ACTION, envelope);

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
                sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.clear();
                editor.apply();
                Intent i = new Intent(GetDocument.this, MainActivity.class);
                startActivity(i);
                finish();
        }
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        //popupMenu.setS
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_ITEM_1_ID:
                        //Toast.makeText(GetDocument.this, "Item 1 clicked", Toast.LENGTH_SHORT).show();
                        new logout().execute();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popupMenu.getMenu().add(0, MENU_ITEM_1_ID, 0, "Logout");
//        popupMenu.getMenu().add(0, MENU_ITEM_2_ID, 0, "Item 2");
        popupMenu.show();
    }

}
