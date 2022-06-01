package com.example.qa.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;


/**
 * @ClassName: HttpUtil
 * @Author: lerry_li
 * @CreateDate: 2021/01/16
 * @Description http工具类，封装get、post请求等
 */
@Component
public class HttpUtil {

    /**
     * 发送http get请求
     *
     * @param requestURL url
     * @param params     参数
     * @return string
     */
    public String sendHttpGetRequest(String requestURL, MultiValueMap<String, Object> params) {
        RestTemplate restTemplate = new RestTemplate();
        String result = null;
        try {
            result = restTemplate.getForObject(requestURL, String.class, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 发送http post请求
     *
     * @param requestURL url
     * @param params     参数
     * @return string
     */
    public String sendHttpPostRequest(String requestURL, MultiValueMap<String, Object> params) {
        RestTemplate restTemplate = new RestTemplate();
//        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
//        restTemplate.getMessageConverters().set(1, converter);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, headers);
        String result = null;
        try {
            result = restTemplate.postForObject(requestURL, request, String.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }


}
