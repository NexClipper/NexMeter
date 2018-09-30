package org.uengine.meter.billing.kb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uengine.meter.util.HttpUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class KBApi {

    private Logger logger = LoggerFactory.getLogger(KBApi.class);

    @Autowired
    private KBConfig kbConfig;

    @Autowired
    private ObjectMapper objectMapper;

    public Map getCatalog() {
        SimpleDateFormat sm = new SimpleDateFormat("yyyy-MM-dd");
        final String requestDate = sm.format(new Date());
        String method = "GET";
        String path = "/1.0/kb/catalog?requestedDate=" + requestDate;

        Map headers = new HashMap();
        try {
            HttpResponse httpResponse = this.apiRequest(method, path, null, headers);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String result = EntityUtils.toString(httpResponse.getEntity());
                final List<Map> list = objectMapper.readValue(result, List.class);
                return list.get(0);
            } else {
                logger.error("Not found catalog");
                return null;
            }
        } catch (IOException ex) {
            logger.error("Failed to request catalog");
            return null;
        }
    }

    public Map getAccountById(String accountId) {
        String method = "GET";
        String path = "/1.0/kb/accounts/" + accountId;

        Map headers = new HashMap();
        try {
            HttpResponse httpResponse = this.apiRequest(method, path, null, headers);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String result = EntityUtils.toString(httpResponse.getEntity());
                return objectMapper.readValue(result, Map.class);
            } else {
                logger.error("Not found account ");
                return null;
            }
        } catch (IOException ex) {
            logger.error("Failed to request account ");
            return null;
        }
    }

    public Map getAccountByExternalKey(String userName) {
        String method = "GET";
        String path = "/1.0/kb/accounts?externalKey=" + userName;

        Map headers = new HashMap();
        try {
            HttpResponse httpResponse = this.apiRequest(method, path, null, headers);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String result = EntityUtils.toString(httpResponse.getEntity());
                return objectMapper.readValue(result, Map.class);
            } else {
                logger.error("Not found account key");
                return null;
            }
        } catch (IOException ex) {
            logger.error("Failed to request account key");
            return null;
        }
    }

    public List<Map> getAccountBundles(String accountId) {
        String method = "GET";
        String path = "/1.0/kb/accounts/" + accountId + "/bundles?audit=NONE";

        Map headers = new HashMap();
        try {
            HttpResponse httpResponse = this.apiRequest(method, path, null, headers);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                String result = EntityUtils.toString(httpResponse.getEntity());
                return objectMapper.readValue(result, List.class);
            } else {
                logger.error("Not found account bundles");
                return null;
            }
        } catch (IOException ex) {
            logger.error("Failed to request account bundles");
            return null;
        }
    }

    public HttpResponse apiRequest(String method, String path, String data, Map headers) throws IOException {
        Map requiredHeaders = new HashMap();

        String origin = kbConfig.getUsername() + ":" + kbConfig.getPassword();
        final String token = new String(Base64.getEncoder().encode(origin.getBytes()));
        requiredHeaders.put("Authorization", "Basic " + token);
        requiredHeaders.put("Content-Type", "application/json");
        requiredHeaders.put("Accept", "application/json");
        requiredHeaders.put("X-Killbill-ApiKey", kbConfig.getApiKey());
        requiredHeaders.put("X-Killbill-ApiSecret", kbConfig.getApiSecret());
        requiredHeaders.putAll(headers);

        String url = kbConfig.getUrl() + path;
        HttpUtils httpUtils = new HttpUtils();
        HttpResponse httpResponse = httpUtils.makeRequest(method, url, data, requiredHeaders);
        return httpResponse;
    }
}
