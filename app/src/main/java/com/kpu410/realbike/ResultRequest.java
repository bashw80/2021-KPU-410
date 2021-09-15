package com.kpu410.realbike;
import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import java.util.HashMap;
import java.util.Map;

public class ResultRequest extends StringRequest {

    //서버 URL 설정(php 파일 연동)
    final static private String URL = "http://bashw80.ivyro.net/result.php";
    private Map<String, String> map;
    //private Map<String, String>parameters;

    public ResultRequest(String userID,String startLoc,String finishLoc,String moveLength,String moveTime,String date,Response.Listener<String> listener){
        super(Method.POST, URL, listener, null);

        map = new HashMap<>();
        map.put("userID",userID);
        map.put("start",startLoc);
        map.put("finish",finishLoc);
        map.put("date",date);
        map.put("length",moveTime);
        map.put("time",moveLength);

    }

    @Override
    protected Map<String, String>getParams() throws AuthFailureError {
        return map;
    }
}