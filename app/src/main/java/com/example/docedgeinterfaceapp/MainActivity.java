package com.example.docedgeinterfaceapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUsername;
    private EditText editTextPassword;
    private EditText editTextUrl;
    private String username;
    private String password;
    private String url;
    private Button buttonLogin;

    // WSDL URL
    //private static final String WSDL_URL = "https://docedge.pericent.com/services/Auth?wsdl";
    private static String WSDL_URL = "";

    // Namespace
    private static final String NAMESPACE = "http://auth.webservice.docedge.com/";

    // Method name
    private static final String METHOD_NAME = "login";

    // SOAP Action
    private static final String SOAP_ACTION = "http://auth.webservice.docedge.com/AuthService/login";

    public static final String MyPREFERENCES = "MyPrefs" ;
    public static final String user_Sid = "sid";
    public static final String user_name = "username";
    public static final String user_password = "password";
    public static final String server_url = "serverurl";
    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //finishAffinity();

        System.out.println("main activity +++++++++++++++++++");
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextUrl = findViewById(R.id.editTextUrl);
        buttonLogin = findViewById(R.id.buttonLogin);
        sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        if(sharedpreferences.contains(user_Sid))
        {
            //removeLauncherIntentFilter();
            if (getIntent() != null && Intent.ACTION_SEND.equals(getIntent().getAction())) {
                Intent sendIntent = new Intent(getIntent());
                String sids = sharedpreferences.getString(user_Sid, "");
                String urls = sharedpreferences.getString(server_url, "");
                sendIntent.putExtra("sid", sids);
                sendIntent.putExtra("url", urls);
                sendIntent.setClass(MainActivity.this, GetDocument.class);
                startActivity(sendIntent);

            }else{
                //Intent intent = new Intent(MainActivity.this, GetDocument.class);
                Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                String sids = sharedpreferences.getString(user_Sid, "");
                String urls = sharedpreferences.getString(server_url, "");
                intent.putExtra("sid", sids);
                intent.putExtra("url", urls);
                startActivity(intent);
            }
        }


        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = editTextUsername.getText().toString().trim();
                password = editTextPassword.getText().toString().trim();
                url = editTextUrl.getText().toString().trim();
                WSDL_URL = url + "/services/Auth?wsdl";

                if (!username.isEmpty() && !password.isEmpty() && !url.isEmpty()) {
                    new SOAPRequestTask().execute(username, password, url);
                } else {
                    Toast.makeText(MainActivity.this, "Please enter proper details", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private class SOAPRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                // Create SOAP request
                String username = params[0];
                String password = params[1];
                String url = params[2];
                SoapObject request = new SoapObject(NAMESPACE, METHOD_NAME);
                PropertyInfo usernameInfo = new PropertyInfo();
                usernameInfo.setName("username");
                usernameInfo.setValue(username);
                usernameInfo.setType(String.class);
                request.addProperty(usernameInfo);

                PropertyInfo passwordInfo = new PropertyInfo();
                passwordInfo.setName("password");
                passwordInfo.setValue(password);
                passwordInfo.setType(String.class);
                request.addProperty(passwordInfo);

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

            if (result != null) {
               // removeLauncherIntentFilter();
                Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(user_Sid, result);
                editor.putString(user_name, username);
                editor.putString(user_password, password);
                editor.putString(server_url, url);
                editor.commit();

                if (getIntent() != null && Intent.ACTION_SEND.equals(getIntent().getAction())) {
                    Intent sendIntent = new Intent(getIntent());
                    sendIntent.putExtra("sid", result);
                    sendIntent.putExtra("url", url);
                    sendIntent.setClass(MainActivity.this, GetDocument.class);
                    startActivity(sendIntent);

                }else{
                    //Intent intent = new Intent(MainActivity.this, GetDocument.class);
                    Intent intent = new Intent(MainActivity.this, WelcomeActivity.class);
                    intent.putExtra("sid", result); // Example: sending a String
                    intent.putExtra("url", url);
                    startActivity(intent);
                }

            } else {
                Toast.makeText(MainActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
            }
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
                        // Exit the app
                        finish();
                        System.exit(0);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

}