package org.uengine.meter.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.uengine.meter.billing.kb.KBConfig;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by uengine on 2016. 4. 22..
 */
@Component
@Order(1)
public class KBRestFilter extends GenericFilterBean {

    @Autowired
    private KBConfig kbConfig;


    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;
        String requestURI = request.getRequestURI();

        if (request.getMethod().equals(HttpMethod.OPTIONS.toString())) {
            chain.doFilter(req, res);
        } else {
            if (requestURI.startsWith("/1.0/kb") ||
                    requestURI.startsWith("/plugins")) {
                try {
                    doKBProxy(request, response);
                    return;
                } catch (Exception ex) {
                    response.setStatus(400);
                    this.addCors(response);
                    return;
                }
            } else {
                //this.addCors(response);
                chain.doFilter(req, res);
            }
        }
    }

    @Override
    public void destroy() {

    }

    private void doKBProxy(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map requiredHeaders = new HashMap();

        String origin = kbConfig.getUsername() + ":" + kbConfig.getPassword();
        final String token = new String(Base64.getEncoder().encode(origin.getBytes()));
        requiredHeaders.put("Authorization", "Basic " + token);
        requiredHeaders.put("Content-Type", "application/json");
        requiredHeaders.put("Accept", "application/json");
        requiredHeaders.put("X-Killbill-ApiKey", kbConfig.getApiKey());
        requiredHeaders.put("X-Killbill-ApiSecret", kbConfig.getApiSecret());

        ProxyRequest proxyRequest = new ProxyRequest();
        proxyRequest.setRequest(request);
        proxyRequest.setResponse(response);

        proxyRequest.setHost(kbConfig.getUrl());
        proxyRequest.setPath(request.getRequestURI());
        proxyRequest.setHeaders(requiredHeaders);
        proxyRequest.setResponseHeaders(this.addReponseHeaders());

        new ProxyService().doProxy(proxyRequest);
    }

    private Map<String, String> addReponseHeaders() {
        Map<String, String> map = new HashMap();

        map.put("Access-Control-Allow-Origin", "*");
        map.put("Access-Control-Allow-Methods", "POST, GET, DELETE, PUT, OPTIONS");
        map.put("Access-Control-Max-Age", "3600");
        map.put("Access-Control-Allow-Headers", "x-requested-with, origin, content-type, accept, " +
                "authorization, X-organization-id, X-Killbill-CreatedBy, X-Killbill-Reason, X-Killbill-Comment, Location");
        return map;
    }

    private void addCors(HttpServletResponse response) {
        Map<String, String> map = this.addReponseHeaders();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            response.setHeader(entry.getKey(), entry.getValue());
        }
    }
}
