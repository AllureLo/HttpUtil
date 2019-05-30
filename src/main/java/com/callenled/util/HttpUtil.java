package com.callenled.util;

import com.callenled.http.bean.BaseResponseObject;
import com.callenled.http.exception.HttpUtilClosableException;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpRequestRetryHandler;
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
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.SocketException;
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
     * http请求方式
     */
    public enum Https {
        /**
         * post
         */
        POST,

        /**
         * get
         */
        GET;
    }

    /**
     * 编码格式 发送编码格式统一用 utf-8
     */
    private static final String CHARSET_UTF8 = "UTF-8";

    /**
     * 设置建立连接超时时间 单位 毫秒(ms)
     */
    private static final int CONNECT_TIMEOUT = 10000;

    /**
     * 设置数据传输处理时间 单位 毫秒(ms)
     */
    private static final int SOCKET_TIMEOUT = 10000;

    /**
     * 设置请求时间 单位 毫秒(ms)
     */
    private static final int CONNECT_REQUEST_TIMEOUT = 1000;

    /**
     * 连接池最大并发连接数
     */
    private static final int MAX_TOTAL = 128;

    /**
     * 单路由最大并发数
     */
    private static final int MAX_PER_ROUTE = 128;

    /**
     * 获取SSL上下文对象,用来构建SSL Socket连接
     *
     * @param certPath   SSL文件
     * @param certPass   SSL密码
     * @return SSL上下文对象
     */
    public static SSLContext getSSLContext(String certPath, String certPass) throws HttpUtilClosableException {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            // 证书文件流
            File file = new File(certPath);
            if (!file.exists()) {
                throw new HttpUtilClosableException("证书不存在");
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
            throw new HttpUtilClosableException(e.getMessage(), e);
        }
    }

    /**
     * httpclient连接配置
     *
     * @return
     */
    private static RequestConfig getRequestConfig() {
        return getRequestConfig(CONNECT_TIMEOUT, SOCKET_TIMEOUT);
    }

    /**
     * httpclient连接配置
     *
     * @param connect 建立连接超时时间
     * @param socket 数据传输处理时间
     * @return
     */
    private static RequestConfig getRequestConfig(int connect, int socket) {
        return RequestConfig.custom()
                //建立连接的timeout时间
                .setConnectTimeout(connect)
                //数据传输处理时间 tcp
                .setSocketTimeout(socket)
                //从连接池中后去连接的timeout时间
                .setConnectionRequestTimeout(CONNECT_REQUEST_TIMEOUT)
                .setExpectContinueEnabled(true)
                //重点参数，在请求之前校验链接是否有效
                .setStaleConnectionCheckEnabled(true)
                .build();
    }

    /**
     * httpclient连接池的配置
     *
     * @param maxTotal 设置最大连接数
     * @param maxPerRoute 设置单路由链接数
     * @return
     */
    private static PoolingHttpClientConnectionManager getConnectionManager(int maxTotal, int maxPerRoute) {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        //设置最大连接数
        connectionManager.setMaxTotal(maxTotal);
        //设置单路由链接数
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        return connectionManager;
    }

    /**
     * httpclient连接池的配置
     *
     * @param sslContext SSL证书
     * @param maxTotal 设置最大连接数
     * @param maxPerRoute 设置单路由链接数
     * @return
     */
    private static PoolingHttpClientConnectionManager getConnectionManager(SSLContext sslContext, int maxTotal, int maxPerRoute) {
        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext
                ,null, null, NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", csf)
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
        //设置最大连接数
        connectionManager.setMaxTotal(maxTotal);
        //设置单路由链接数
        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        return connectionManager;
    }

    /**
     * 自定义策略 重连
     *
     * @param count 重连次数
     * @return
     */
    private static HttpRequestRetryHandler getRetryHandler(int count) {
        return (exception, executionCount, context) -> {
            if (executionCount > count) {
                return false;
            }
            if (exception instanceof NoHttpResponseException) {
                return true;
            }
            else {
                return exception instanceof SocketException;
            }
        };
    }

    /**
     * 从连接池中获取 CloseableHttpClient(不带证书)
     */
    private static CloseableHttpClient getHttpClient(RequestConfig requestConfig, PoolingHttpClientConnectionManager connectionManager) {
        return getHttpClient(requestConfig, connectionManager, new DefaultHttpRequestRetryHandler(0, false));
    }

    /**
     * 从连接池中获取 CloseableHttpClient(不带证书)
     */
    private static CloseableHttpClient getHttpClient(RequestConfig requestConfig, PoolingHttpClientConnectionManager connectionManager,
                                                     HttpRequestRetryHandler retryHandler) {
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .setRetryHandler(retryHandler)
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
    private static URI getUrl(String url, Map<String, Object> params) throws HttpUtilClosableException {
        // 创建访问的地址
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            if (params != null) {
                Set<Map.Entry<String, Object>> entrySet = params.entrySet();
                for (Map.Entry<String, Object> entry : entrySet) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        uriBuilder.setParameter(key, String.valueOf(value));
                    }
                }
            }
            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new HttpUtilClosableException(e.getMessage(), e);
        }
    }

    /**
     * 设置请求头
     *
     * @param httpRequest http请求
     * @param headers     请求头参数
     */
    private static HttpRequestBase setHeader(HttpRequestBase httpRequest, Map<String, String> headers) {
        // 设置请求头
        if (headers != null) {
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
    private static HttpGet doGet(String url, Map<String, String> headers, Map<String, Object> params) throws HttpUtilClosableException {
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
    private static HttpPost doPost(String url, Map<String, String> headers, Map<String, Object> params, Object jsonObject, String contentType) throws HttpUtilClosableException {
        // 创建httpPost
        HttpPost httpPost;
        if (jsonObject != null && params != null) {
            // 创建访问的地址
            URI uri = getUrl(url, params);
            httpPost = new HttpPost(uri);
        } else {
            httpPost = new HttpPost(url);
        }
        // 设置请求参数
        if (jsonObject != null) {
            String json = GsonUtil.gsonString(jsonObject);
            // 请求头
            httpPost.setHeader(HTTP.CONTENT_TYPE, "application/json");
            // 设置参数
            StringEntity entity = new StringEntity(json, CHARSET_UTF8);
            if (contentType != null) {
                entity.setContentType(contentType);
            }
            httpPost.setEntity(entity);
        } else if (params != null) {
            // 设置参数
            List<NameValuePair> nvp = new ArrayList<>();
            Set<Map.Entry<String, Object>> entrySet = params.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null) {
                    nvp.add(new BasicNameValuePair(key, String.valueOf(value)));
                }
            }
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(nvp, CHARSET_UTF8));
            } catch (UnsupportedEncodingException e) {
                throw new HttpUtilClosableException(e.getMessage(), e);
            }
        }
        return (HttpPost) setHeader(httpPost, headers);
    }

    /**
     * http全局配置
     *
     * @return
     */
    public static HttpConfig config() {
        return new HttpConfig(MAX_TOTAL, MAX_PER_ROUTE);
    }

    /**
     * http全局配置
     *
     * @param maxTotal 连接池最大连接数量
     * @param maxPerRoute 单路由最大并发数
     * @return
     */
    public static HttpConfig config(int maxTotal, int maxPerRoute) {
        return new HttpConfig(maxTotal, maxPerRoute);
    }

    /**
     * 默认配置
     *
     * @return
     */
    public static Builder builder() {
        //默认连接时间20000
        RequestConfig requestConfig = HttpUtil.getRequestConfig();
        //默认开启最大连接数128
        PoolingHttpClientConnectionManager connectionManager = HttpUtil.getConnectionManager(MAX_TOTAL, MAX_PER_ROUTE);
        return new Builder(HttpUtil.getHttpClient(requestConfig, connectionManager));
    }

    public static class HttpConfig {
        /**
         * httpclient全局配置
         */
        private RequestConfig requestConfig;

        /**
         * httpclient连接池的配置
         */
        private PoolingHttpClientConnectionManager connectionManager;

        /**
         * 连接策略
         */
        private HttpRequestRetryHandler retryHandler;

        /**
         * 连接池最大连接数量
         */
        private int maxTotal;

        /**
         * 单路由最大并发数
         */
        private int maxPerRoute;

        private HttpConfig(int maxTotal, int maxPerRoute) {
            this.maxTotal = maxTotal;
            this.maxPerRoute = maxPerRoute;
        }

        /**
         * 设置链接超时
         *
         * @param connect 链接时间
         * @param socket 传输时间
         * @return
         */
        public HttpConfig setTimeout(int connect, int socket) {
            this.requestConfig = HttpUtil.getRequestConfig(connect, socket);
            return this;
        }

        /**
         * 从连接池中获取 CloseableHttpClient(带证书)
         *
         * @param sslContext 证书
         */
        public HttpConfig setSSLContext(SSLContext sslContext) {
            if (sslContext == null) {
                try {
                    throw new HttpUtilClosableException("证书为空");
                } catch (HttpUtilClosableException e) {
                    e.printStackTrace();
                }
            }
            this.connectionManager = HttpUtil.getConnectionManager(sslContext, this.maxTotal, this.maxPerRoute);
            return this;
        }

        /**
         * 从连接池中获取 CloseableHttpClient(带证书)
         *
         * @param certPath   SSL文件
         * @param certPass   SSL密码
         */
        public HttpConfig setSSLContext(String certPath, String certPass) {
            SSLContext sslContext = null;
            try {
                sslContext = HttpUtil.getSSLContext(certPath, certPass);
            } catch (HttpUtilClosableException e) {
                e.printStackTrace();
            }
            return setSSLContext(sslContext);
        }

        /**
         * 开启重连
         *
         * @return
         */
        public HttpConfig setOnRetryTimes() {
            this.retryHandler = HttpUtil.getRetryHandler(3);
            return this;
        }

        /**
         * 开启重连
         *
         * @return
         */
        public HttpConfig setOnRetryTimes(int count) {
            this.retryHandler = HttpUtil.getRetryHandler(count);
            return this;
        }

        /**
         * 开启http请求
         *
         * @return
         */
        public Builder builder() {
            //请求client
            if (this.requestConfig == null) {
                this.requestConfig = HttpUtil.getRequestConfig();
            }
            if (this.connectionManager == null) {
                this.connectionManager = HttpUtil.getConnectionManager(this.maxTotal, this.maxPerRoute);
            }
            if (this.retryHandler == null) {
                this.retryHandler = new DefaultHttpRequestRetryHandler(0, false);
            }
            return new Builder(HttpUtil.getHttpClient(this.requestConfig, this.connectionManager, this.retryHandler));
        }

    }

    /**
     * 构造http请求函数
     */
    public static class Builder {

        /**
         * 请求参数(key-value格式)
         */
        private Map<String, Object> params;

        /**
         * 请求参数(json格式)
         */
        private Object jsonObject;

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
         * httpclient客户端
         */
        private CloseableHttpClient httpClient;

        /**
         * http响应参数
         */
        private CloseableHttpResponse httpResponse;

        private Builder(CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
        }

        /**
         * 添加参数
         *
         * @param key 键
         * @param value 值
         * @return
         */
        public Builder addParams(String key, Object value) {
            if (this.params == null) {
                this.params = new HashMap<>(5);
            }
            this.params.put(key, value);
            return this;
        }

        /**
         * 添加参数
         *
         * @param params 参数
         * @return
         */
        public Builder addParams(Map<String, Object> params) {
            if (this.params == null) {
                this.params = new HashMap<>(5);
            }
            this.params.putAll(params);
            return this;
        }

        /**
         * 添加参数
         *
         * @param object 参数
         * @return
         */
        public Builder ajaxJson(Object object) {
            this.jsonObject = object;
            return this;
        }

        /**
         * 添加请求头
         *
         * @param key 键
         * @param value 值
         * @return
         */
        public Builder addHeaders(String key, String value) {
            if (this.headers == null) {
                this.headers = new HashMap<>(5);
            }
            this.headers.put(key, value);
            return this;
        }

        /**
         * 设置请求格式
         *
         * @param contentType 请求格式
         * @return
         */
        public Builder setContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * MD5签名 (目前不支持json格式)
         *
         * @param secretKey 签名秘钥
         * @param keys 需要签名的参数
         * @return
         */
        public Builder signWithMD5(String secretKey, String ...keys) {
            if (keys == null || keys.length == 0) {
                String sign = SignUtil.createSign(this.params, secretKey);
                this.params.put("sign", sign);
            } else {
                Map<String, Object> signMap = new HashMap<>(5);
                for (String key : keys) {
                    Object value = this.params.get(key);
                    if (value != null) {
                        signMap.put(key, value);
                    }
                }
                String sign = SignUtil.createSign(signMap, secretKey);
                this.params.put("sign", sign);
            }
            return this;
        }

        /**
         * http请求
         * @param url 请求地址
         * @param https 请求方法
         * @return
         */
        public Builder doHttp(String url, Https https)  {
            switch (https) {
                case POST:
                    return doPost(url);
                case GET:
                    return doGet(url);
                default:
                    return this;
            }
        }

        /**
         * get请求方法 带请求头和请求参数
         *
         * @param url     请求地址
         * @return HttpGet
         */
        public Builder doGet(String url) {
            try {
                this.httpRequest = HttpUtil.doGet(url, this.headers, this.params);
            } catch (HttpUtilClosableException e) {
                e.printStackTrace();
            }
            return doHttp();
        }

        /**
         * post请求 (key-value格式)
         * @param url 请求地址
         * @return HttpPost
         */
        public Builder doPost(String url) {
            try {
                this.httpRequest = HttpUtil.doPost(url, this.headers, this.params, this.jsonObject, this.contentType);
            } catch (HttpUtilClosableException e) {
                e.printStackTrace();
            }
            return doHttp();
        }

        /**
         * http请求
         * @return
         */
        private Builder doHttp() {
            try {
                //响应
                this.httpResponse = this.httpClient.execute(this.httpRequest);
                //响应状态
                StatusLine status = this.httpResponse.getStatusLine();
                if (status.getStatusCode() != HttpStatus.SC_OK) {
                    throw new HttpUtilClosableException(status.getReasonPhrase());
                }
            } catch (IOException | HttpUtilClosableException e) {
                e.printStackTrace();
            }
            return this;
        }

        /**
         * 返回数据 byte
         * @return
         */
        public byte[] toByte() {
            byte[] bytes = null;
            if (this.httpResponse != null) {
                try {
                    bytes = EntityUtils.toByteArray(this.httpResponse.getEntity());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        this.httpResponse.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bytes;
        }

        /**
         * 返回数据 InputStream
         * @return
         */
        public InputStream toInput() {
            InputStream input = null;
            if (this.httpResponse != null) {
                try {
                    input = this.httpResponse.getEntity().getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        this.httpResponse.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return input;
        }

        /**
         * 返回数据 String
         * @return
         */
        public String toJson() {
            String json = "";
            if (this.httpResponse != null) {
                try {
                    json = EntityUtils.toString(this.httpResponse.getEntity(), CHARSET_UTF8);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        this.httpResponse.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return json;
        }

        /**
         * 返回参数 map格式
         *
         * @return
         */
        public <T> Map<String, T> toMap() {
            return GsonUtil.gsonToMaps(toJson());
        }

        /**
         * 返回参数 对象
         *
         * @return
         */
        public <T> T toObject(Class<T> clazz) {
            return GsonUtil.gsonToBean(toJson(), clazz);
        }

        /**
         * 返回参数 list格式
         *
         * @return
         */
        public <T> List<T> toArray(Class<T> clazz) {
            return GsonUtil.gsonToArray(toJson(), clazz);
        }

        /**
         * 返回参数 response格式
         *
         * @return
         */
        public <S, T extends BaseResponseObject> T toResponseObject(Class<? extends BaseResponseObject> clazz, Class<S> cls) {
            return GsonUtil.gsonToResponseObject(toJson(), clazz, cls);
        }

        /**
         * 返回参数 response array格式
         *
         * @return
         */
        public <S, T extends BaseResponseObject<List<S>>> T toResponseArray(Class<? extends BaseResponseObject> clazz, Class<S> cls) {
            return GsonUtil.gsonToResponseArray(toJson(), clazz, cls);
        }
    }
}
