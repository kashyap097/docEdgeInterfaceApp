package com.example.docedgeinterfaceapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.PropertyInfo;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

public class WelcomeActivity extends AppCompatActivity {

    TextView logoutLink;
    SharedPreferences sharedpreferences;
    private static final String LogoutNAMESPACE = "http://auth.webservice.docedge.com/";

    // Method name
    private static final String Logout_METHOD_NAME = "logout";

    // SOAP Action
    private static final String Logout_SOAP_ACTION = "http://auth.webservice.docedge.com/AuthService/logout";

    public static final String MyPREFERENCES = "MyPrefs" ;
    private String url = "";

    private static String sid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        logoutLink = findViewById(R.id.logoutLink);
        logoutLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Redirect to Login screen
                sid = getIntent().getStringExtra("sid");
                url = getIntent().getStringExtra("url");
                System.out.println("sid ++++++++"+sid);
                System.out.println("url ++++++++"+url);
                new logout().execute();
            }
        });
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
            Intent i = new Intent(WelcomeActivity.this, MainActivity.class);
            startActivity(i);
            finish();
        }
    }
}
