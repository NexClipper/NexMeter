package org.uengine.meter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;

import java.util.*;

public class MeterGen {


    public static void main(String[] args) {
        Map requiredHeaders = new HashMap();
        requiredHeaders.put("Content-Type", "application/json");
        final ObjectMapper objectMapper = new ObjectMapper();

        final long monthHours = 24L * 30L;
        //long current = new Date().getTime() - (monthHours * 3600L * 1000L);
        long current = new Date().getTime();

        for (int i = 0; i < monthHours; i++) {
            current = current + (3600L * 1000L);

            final ArrayList<Map> records = new ArrayList<>();
            Map analytics = new HashMap();
            analytics.put("unit", "analytics");
            analytics.put("user", "darkgodarkgo@gmail.com");
            analytics.put("amount", (long) (Math.random() * 150 + 1));
            analytics.put("time", current);
            records.add(analytics);

            Map host = new HashMap();
            host.put("unit", "host");
            host.put("user", "darkgodarkgo@gmail.com");
            host.put("amount", (long) (Math.random() * 25 + 1));
            host.put("time", current);
            records.add(host);

            try {
                final String value = objectMapper.writeValueAsString(records);
                String url = "http://121.167.146.57:12004/meter/record/json";
                HttpUtils httpUtils = new HttpUtils();
                HttpResponse httpResponse = httpUtils.makeRequest("POST", url, value, requiredHeaders);
            } catch (Exception ex) {

            }
        }

    }

}
