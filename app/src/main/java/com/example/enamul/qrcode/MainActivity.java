package com.example.enamul.qrcode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity {
    Button button;
    Button btnScan;
    EditText editText;
    Date now = new Date();
    DateFormat dateformatter = DateFormat.getDateInstance(DateFormat.SHORT);
    String formattedDate = dateformatter.format(now);
    DateFormat timeformatter = DateFormat.getTimeInstance(DateFormat.SHORT);
    String formattedTime = timeformatter.format(now);
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_qr_readTxt,lat,longi, textAddress,txt_date;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest mLocationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText)findViewById(R.id.editText);
        btnScan = (Button)findViewById(R.id.btnScan);
        tv_qr_readTxt = (TextView) findViewById(R.id.tv_qr_readTxt);
        txt_date = (TextView) findViewById(R.id.txt_date);

        lat = findViewById(R.id.lat);
        longi = findViewById(R.id.longi);
        textAddress = findViewById(R.id.textAddress);
//        progressBar = findViewById(R.id.progressBar);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });

        txt_date.setText(String.valueOf(formattedDate+" à "+formattedTime));
        textAddress.setText(String.valueOf("loading..."));
        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_FINE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateGPS();
            } else {
                Toast.makeText(this, "This app requires permission to be granted", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void updateGPS() {
        fusedLocationProviderClient= getFusedLocationProviderClient(MainActivity.this);
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            mLocationRequest = new LocationRequest();
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY );
            mLocationRequest.setInterval(500);
            mLocationRequest.setFastestInterval(500);
            getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            // do work here
                            onLocationChanged(locationResult.getLastLocation());
                        }
                    },
                    Looper.myLooper());

            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if(location != null){
                        updateUIValues(location);
                    }

                    else
                        {lat.setText(String.valueOf("loading..."));
                        longi.setText(String.valueOf("loading..."));
                        }
                }
            });
        }
        else{
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    private void onLocationChanged(Location lastLocation) {
        lat.setText(String.valueOf(lastLocation.getLatitude()));
        longi.setText(String.valueOf(lastLocation.getLongitude()));
        updateUIValues(lastLocation);
    }

    private void updateUIValues(Location location) {
        if(textAddress.getText() == String.valueOf("loading...")){
            Geocoder gcd = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            } catch (IOException e) {
                e.printStackTrace();
            }
            textAddress.setText(String.valueOf(addresses.get(0).getFeatureName()+", "  + addresses.get(0).getThoroughfare()+", " + addresses.get(0).getLocality()));
            Looper.myLooper();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Log.e("Scan*******", "Cancelled scan");
            } else {
                Log.e("Scan", "Scanned");
                tv_qr_readTxt.setText(result.getContents());
                try {
                    SmsManager.getDefault().sendTextMessage("0631711796",
                            null, "La benne n°"+String.valueOf(tv_qr_readTxt.getText())+"\n"+String.valueOf(txt_date.getText())+"\n"+String.valueOf(textAddress.getText()),null,null);
                    Toast.makeText(getApplicationContext(),"Envoi reussi!",Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(),"Echec de l'envoie,réessayer!",Toast.LENGTH_LONG).show();
                }
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}