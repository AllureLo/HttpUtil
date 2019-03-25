package com.callenld.util;

import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.util.*;

/**
 * @Author: Callenld
 * @Date: 19-3-19
 */
public class HttpUtil {

    /**
     * 编码格式 发送编码格式统一用 utf-8
     */
    private static final String CHARSET_UTF8 = "UTF-8";

    /**
     * 设置建立连接超时时间 单位 毫秒(ms)
     */
    private static final int CONNECT_TIMEOUT = 6000;

    /**
     * 设置响应时间 单位 毫秒(ms)
     */
    private static final int SOCKET_TIMEOUT = 6000;

    /**
     * 设置请求时间 单位 毫秒(ms)
     */
    private static final int CONNECT_REQUEST_TIMEOUT = 1000;

    /**
     * 单列模式
     */
    private volatile static HttpUtil instance = null;

    /**
     * 单列模式
     */
    private static HttpUtil getInstance() {
        if (Objects.isNull(instance)) {
            instance = new HttpUtil();
        }
        return instance;
    }

    /**
     * 私有化构造函数
     */
    private HttpUtil() {
        //请求器的配置
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIMEOUT)
                .build();
    }

    /**
     * 请求器的配置
     */
    private volatile RequestConfig requestConfig;

    /**
     * 获取SSL上下文对象,用来构建SSL Socket连接
     *
     * @param certPath   SSL文件
     * @param certPass   SSL密码
     * @return SSL上下文对象
     */
    public static SSLContext getSSLContext(String certPath, String certPass) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            // 证书文件流
            File file = new File(certPath);
            if (!file.exists()) {
                throw new Exception("证书不存在");
            }
            InputStream input = new FileInputStream(file);
            keyStore.load(input, certPass.toCharArray());
            // 相信自己的CA和所有自签名的证书
            return SSLContexts.custom()
                    //忽略掉对服务器端证书的校验
                    //.loadTrustMaterial((TrustStrategy) (chain, authType) -> true)
                    //加载服务端提供的truststore(如果服务器提供truststore的话就不用忽略对服务器端证书的校验了)
                    .loadKeyMaterial(keyStore, certPass.toCharArray()).build();
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * httpclient连接池的配置
     */
    private PoolingHttpClientConnectionManager getConnectionManager(SSLContext sslContext) {
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext
                ,null, null, NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", csf)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        //最大连接数128
        connectionManager.setMaxTotal(256);
        //路由链接数128
        connectionManager.setDefaultMaxPerRoute(128);
        return connectionManager;
    }

    /**
     * 从连接池中获取 CloseableHttpClient(不带证书)
     */
    private CloseableHttpClient getHttpClient() {
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();
    }

    /**
     * 从连接池中获取 CloseableHttpClient(带证书)
     *
     * @param sslContext 证书
     */
    private CloseableHttpClient getHttpClient(SSLContext sslContext) {
        return HttpClients.custom()
                .setConnectionManager(getConnectionManager(sslContext))
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()
                .build();
    }

    /**
     * 创建访问的地址 拼接url
     *
     * @param url    基础url
     * @param params 请求参数
     * @return URI
     */
    private URI getUrl(String url, Map<String, String> params) {
        // 创建访问的地址
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            if (Objects.nonNull(params)) {
                Set<Map.Entry<String, String>> entrySet = params.entrySet();
                for (Map.Entry<String, String> entry : entrySet) {
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * 设置请求头
     *
     * @param httpRequest http请求
     * @param headers     请求头参数
     */
    private HttpRequestBase setHeader(HttpRequestBase httpRequest, Map<String, String> headers) {
        // 设置请求头
        if (Objects.nonNull(headers)) {
            Set<Map.Entry<String, String>> entrySet = headers.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                // 设置请求头到 HttpRequestBase
                httpRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return httpRequest;
    }

    /**
     * get请求方法 带请求头和请求参数
     *
     * @param url     请求地址
     * @param headers 请求头
     * @param params  请求参数
     * @return HttpGet
     */
    private HttpGet doGet(String url, Map<String, String> headers, Map<String, String> params){
        // 创建访问的地址
        URI uri = getUrl(url, params);
        // 创建http对象
        HttpGet httpGet = new HttpGet(uri);
        // 设置请求头
        return (HttpGet)setHeader(httpGet, headers);
    }

    /**
     * post请求 (key-value格式)
     * @param url     请求地址
     * @param headers 请求头
     * @param params  请求参数
     * @return HttpPost
     */
    private HttpPost doPost(String url, Map<String, String> headers, Map<String, String> params, String json, String contentType){
        // 创建httpPost
        HttpPost httpPost;
        if (Objects.nonNull(json) && Objects.nonNull(params)) {
            // 创建访问的地址
            URI uri = getUrl(url, params);
            httpPost = new HttpPost(uri);
        } else {
            httpPost = new HttpPost(url);
        }
        // 设置请求参数
        if (Objects.nonNull(json)) {
            // 请求头
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/json");
            // 设置参数
            StringEntity entity = new StringEntity(json, CHARSET_UTF8);
            if (Objects.nonNull(contentType)) {
                entity.setContentType(contentType);
            }
            httpPost.setEntity(entity);
        } else if (Objects.nonNull(params)) {
            // 设置参数
            List<NameValuePair> nvp = new ArrayList<>();
            Set<Map.Entry<String, String>> entrySet = params.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                String name = entry.getKey();
                String value = entry.getValue();
                nvp.add(new BasicNameValuePair(name, value));
            }
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nvp, CHARSET_UTF8));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
        return (HttpPost) setHeader(httpPost, headers);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造http请求函数
     */
    public static class Builder {

        /**
         * 请求参数(key-value格式)
         */
        private Map<String, String> params;

        /**
         * 请求参数(json格式)
         */
        private String json;

        /**
         * 请求头
         */
        private Map<String, String> headers;

        /**
         * 参数返回格式
         */
        private String contentType;

        /**
         * http请求方式
         */
        private HttpUriRequest httpRequest;

        /**
         * http请求连接
         */
        private CloseableHttpClient httpClient;

        /**
         * http响应参数
         */
        private CloseableHttpResponse httpResponse;

        /**
         * httpUtil工具类实例化
         */
        private HttpUtil httpUtil;

        /**
         * 初始化
         */
        public Builder() {
            this.httpUtil = HttpUtil.getInstance();
        }

        /**
         * 添加参数
         */
        public Builder addParams(String key, String value) {
            if (Objects.isNull(this.params)) {
                this.params = new HashMap<>(5);
            }
            this.params.put(key, value);
            return this;
        }

        /**
         * 添加参数
         */
        public Builder ajaxJson(String json) {
            this.json = json;
            return this;
        }

        /**
         * 添加请求头
         */
        public Builder addHeaders(String key, String value) {
            if (Objects.isNull(this.headers)) {
                this.headers = new HashMap<>(5);
            }
            this.headers.put(key, value);
            return this;
        }

        /**
         * 设置返回格式
         */
        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * 从连接池中获取 CloseableHttpClient(不带证书)
         */
        public Builder getHttpClient() {
            this.httpClient = this.httpUtil.getHttpClient();
            return this;
        }

        /**
         * 从连接池中获取 CloseableHttpClient(带证书)
         *
         * @param sslContext 证书
         */
        public Builder getHttpClient(SSLContext sslContext) {
            this.httpClient = this.httpUtil.getHttpClient(sslContext);
            return this;
        }

        /**
         * 从连接池中获取 CloseableHttpClient(带证书)
         *
         * @param certPath   SSL文件
         * @param certPass   SSL密码
         */
        public Builder getHttpClient(String certPath, String certPass) {
            SSLContext sslContext = HttpUtil.getSSLContext(certPath, certPass);
            this.httpClient = this.httpUtil.getHttpClient(sslContext);
            return this;
        }

        /**
         * get请求方法 带请求头和请求参数
         *
         * @param url     请求地址
         * @return HttpGet
         */
        public Builder doGet(String url){
            this.httpRequest = this.httpUtil.doGet(url, this.headers, this.params);
            return doHttp();
        }

        /**
         * post请求 (key-value格式)
         * @param url 请求地址
         * @return HttpPost
         */
        public Builder doPost(String url){
            this.httpRequest = this.httpUtil.doPost(url, this.headers, this.params, this.json, this.contentType);
            return doHttp();
        }

        /**
         * 返回数据
         * @return
         */
        public byte[] toByte() {
            try {
                return EntityUtils.toByteArray(this.httpResponse.getEntity());
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } finally {
                try {
                    if (this.httpResponse != null) {
                        this.httpResponse.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 返回数据
         * @return
         */
        public InputStream toInput() {
            try {
                return this.httpResponse.getEntity().getContent();
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } finally {
                try {
                    if (this.httpResponse != null) {
                        this.httpResponse.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 返回数据
         * @return
         */
        public String toJson() {
            try {
                return EntityUtils.toString(this.httpResponse.getEntity());
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            } finally {
                try {
                    if (this.httpResponse != null) {
                        this.httpResponse.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * http请求
         * @return
         */
        private Builder doHttp() {
            try {
                //请求client
                if (Objects.isNull(this.httpClient)) {
                    this.httpClient = this.httpUtil.getHttpClient();
                }
                this.httpResponse = this.httpClient.execute(this.httpRequest);
                //响应状态
                StatusLine status = this.httpResponse.getStatusLine();
                if (status.getStatusCode() != HttpStatus.SC_OK) {
                    throw new IllegalArgumentException(status.getReasonPhrase());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            return this;
        }
    }
}
