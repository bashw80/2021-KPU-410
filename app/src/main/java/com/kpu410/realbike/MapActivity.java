// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.kpu410.realbike;

import com.google.android.gms.common.server.converter.StringToIntConverter;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaOrientation;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

/**
 * This shows how to create a simple activity with streetview
 */
public class MapActivity extends AppCompatActivity {
    private static final String API_KEY="AIzaSyAIgidUd3VceMR-6DnCFvrFNyySINLiVWo";
    private static final double CIRCLE = 4.11;
    private String strUrl = null;           // EditText + API URL

    private StreetViewPanorama streetViewPanorama;
    private LatLng[][] routeLatLng;

    private int list_len = 0;
    private int route_len = 0;
    private int route2_len = 0;

    private int temp = 0;
    private int count = 0;
    private double[] feet;

    private BluetoothSPP bt;

    private String startLoc;
    private String finishLoc;

    private String moveLength;

    private long start;
    private long end;
    private double moveTime;

    private LatLng startLatLng;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        start = System.currentTimeMillis();

        Intent searchIntent = getIntent();
        startLoc = searchIntent.getStringExtra("startLoc");
        finishLoc = searchIntent.getStringExtra("finishLoc");

        bt = new BluetoothSPP(this); //Initializing

        if (!bt.isBluetoothAvailable()) { //블루투스 사용 불가
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }

        bluetoothConnect();
        jsonPashing();
        routeLatLng[0][0] = startLatLng;

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() { //데이터 수신
            TextView distance = findViewById(R.id.distance);

            public void onDataReceived(byte[] data, String message) {
                String[] array = message.split(",");
                moveLength = array[2];
                //String total = "시속 : ".concat((array[1].concat("km/h, 이동 거리 : ")).concat(array[2].concat("km")));
                distance.setText(message);

                temp = 0;

                String checkArray = array[0];
                int check = Integer.parseInt(checkArray);
                if (check == 0) {
                    count++;
                    if (count == 10) {
                        temp = 1;
                        count = 0;
                    }
                }
                Log.i("배열 확인", check+","+temp+","+routeLatLng.length);

                if (temp == 1) {
                    if(route_len == routeLatLng.length - 1) {
                        finishDrive();
                    } else {
                        if (route2_len == routeLatLng[list_len].length) {
                            route_len++;
                        } else {
                            route2_len++;
                            streetViewPanorama.setPosition(routeLatLng[route_len][route2_len]);
                            Log.i("경로 확인", route_len + " : " + routeLatLng[route_len][route2_len]);
                        }
                    }
                }
            }
        });

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() { //연결됐을 때
            public void onDeviceConnected(String name, String address) {
                Toast.makeText(getApplicationContext()
                        , "Connected to " + name + "\n" + address
                        , Toast.LENGTH_SHORT).show();
            }

            public void onDeviceDisconnected() { //연결해제
                Toast.makeText(getApplicationContext()
                        , "Connection lost", Toast.LENGTH_SHORT).show();
            }

            public void onDeviceConnectionFailed() { //연결실패
                Toast.makeText(getApplicationContext()
                        , "Unable to connect", Toast.LENGTH_SHORT).show();
                finish();
            }
        });


        SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
                (SupportStreetViewPanoramaFragment)
                        getSupportFragmentManager().findFragmentById(R.id.streetviewpanorama);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(
                new OnStreetViewPanoramaReadyCallback() {
                    @Override
                    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
                        streetViewPanorama = panorama;
                        if (savedInstanceState == null) {
                            panorama.setPosition(routeLatLng[0][0]);
                        }
                    }

                });

        Button btnExit = findViewById(R.id.btnExit);
        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                end = System.currentTimeMillis();
                moveTime = (end - start) / 1000;
                Intent resultIntent = new Intent(getApplicationContext(), ResultActivity.class);
                resultIntent.putExtra("moveLength", moveLength);
                resultIntent.putExtra("moveTime", moveTime);
                startActivity(resultIntent);
            }
        });
    }

    public void finishDrive() {
        AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(MapActivity.this);
        dlgBuilder.setTitle("주행 종료");
        dlgBuilder.setMessage("목적지까지의 주행이 완료되었습니다.");
        dlgBuilder.setPositiveButton("결과 페이지로", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                end = System.currentTimeMillis();
                moveTime = (end - start) / 1000;
                Intent resultIntent = new Intent(getApplicationContext(), ResultActivity.class);
                resultIntent.putExtra("moveLength", moveLength);
                resultIntent.putExtra("moveTime", moveTime);
                startActivity(resultIntent);
                }
            });
        AlertDialog dlg = dlgBuilder.create();
        dlg.show();
    }

    public void bluetoothConnect() {
        if (bt.getServiceState() == BluetoothState.STATE_CONNECTED) {
            bt.disconnect();
        } else {
            Intent intent = new Intent(getApplicationContext(), DeviceList.class);
            startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
        }
    }

    public class Task extends AsyncTask<String, Void, String> {

        private String str, receiveMsg;

        @Override
        protected String doInBackground(String... strings) {
            URL url = null;
            try {
                url = new URL(strUrl);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                if (conn.getResponseCode() == conn.HTTP_OK) {
                    InputStreamReader tmp = new InputStreamReader(conn.getInputStream(), "UTF-8");
                    BufferedReader reader = new BufferedReader(tmp);
                    StringBuffer buffer = new StringBuffer();
                    while ((str = reader.readLine()) != null) {
                        buffer.append(str);
                    }
                    receiveMsg = buffer.toString();     // JSON to String

                    reader.close();
                } else {
                    Log.e("통신 오류", "ERRORCODE" + conn.getResponseCode() + "/ URL : " + strUrl);
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return receiveMsg;
        }
    }

    public void jsonPashing() {
        strUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + startLoc + "&destination=" + finishLoc + "&mode=bicycling&departure_time=now" +
                "&alternatives=true&language=Korean&key=" + API_KEY;
        Log.i("url", strUrl);
        String resultText = "값이 없음";

        try {
            resultText = new Task().execute().get();

            JSONObject jsonObject = new JSONObject(resultText);
            String routes = jsonObject.getString("routes");
            JSONArray routesArray = new JSONArray(routes);
            JSONObject subJsonObject = routesArray.getJSONObject(0);

            String legs = subJsonObject.getString("legs");
            JSONArray legArray = new JSONArray(legs);
            JSONObject legJsonObject = legArray.getJSONObject(0);

            String startL = legJsonObject.getString("start_location");
            JSONObject startLObject = new JSONObject(startL);
            double sLat = startLObject.getDouble("lat");
            double sLng = startLObject.getDouble("lng");

            startLatLng = new LatLng(sLat, sLng);

            String steps = legJsonObject.getString("steps");
            JSONArray stepsArray = new JSONArray(steps);
            list_len = stepsArray.length();

            routeLatLng = new LatLng[list_len + 1][];
            routeLatLng[0][0] = startLatLng;
            feet = new double[list_len];

            for (int i = 0; i < list_len; i++) {
                JSONObject stepsObject = stepsArray.getJSONObject(i);

                String endLoc = stepsObject.getString("end_location");
                JSONObject endObject = new JSONObject(endLoc);
                double eLat = endObject.getDouble("lat");
                double eLng = endObject.getDouble("lng");

                String distance = stepsObject.getString("distance");
                JSONObject distanceObject = new JSONObject(distance);
                String[] array = distanceObject.getString(" ").split(",");

                int distanceT;
                if (array[1] == "mi") {
                    distanceT = (int)(Double.parseDouble(array[0]) * 5280);
                } else {
                    distanceT = Integer.parseInt(array[0]);
                }

                feet[i] = distanceT / CIRCLE;

                routeLatLng[i + 1][0] = new LatLng(eLat, eLng);
                routeLatLng[i] = new LatLng[(int)feet[i] + 1];
                double lastLat = routeLatLng[i][0].latitude;
                double lastLng = routeLatLng[i][0].longitude;
                double nowLat = routeLatLng[i + 1][0].latitude;
                double nowLng = routeLatLng[i + 1][0].longitude;

                double diffLat = (nowLat - lastLat) / feet[i];
                double diffLng = (nowLng - lastLng) / feet[i];

                for (int j = 0; j < (feet[i] + 1); j++) {
                    routeLatLng[i + 1][0] = new LatLng(eLat, eLng);

                    routeLatLng[i][j + 1] = new LatLng(lastLat + diffLat, lastLng + diffLng);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

    }
    public void onDestroy() {
        super.onDestroy();
        bt.stopService(); //블루투스 중지
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) { //
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if (!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER); //DEVICE_ANDROID는 안드로이드 기기 끼리
                setup();
            }
        }
    }

    public void setup() {
        Button exit = findViewById(R.id.btnExit); //데이터 전송
        exit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                bt.send("Reset", true);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                setup();
            } else {
                Toast.makeText(getApplicationContext()
                        , "Bluetooth was not enabled."
                        , Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}