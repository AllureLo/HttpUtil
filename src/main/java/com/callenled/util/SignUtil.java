package com.callenled.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @Author: Callenld
 * @Date: 19-5-23
 */
public class SignUtil {

    /**
     * 生成签名
     *
     * @param map 对象参数
     * @param key 签名密钥
     * @return
     */
    public static String createSign(Map<String, Object> map, String key) {
        List<Map.Entry<String, Object>> infoIds = new ArrayList<>(map.entrySet());
        infoIds.sort(Comparator.comparing(o -> (o.getKey())));
        // 构造签名键值对的格式
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : infoIds) {
            String k = entry.getKey();
            Object v = entry.getValue();
            if (null != v && !"".equals(v)
                    && !"sign".equals(k) && !"key".equals(k)) {
                sb.append(k).append("=").append(v).append("&");
            }
        }
        //最后加密时添加商户密钥，由于key值放在最后，所以不用添加到SortMap里面去，单独处理，编码方式采用UTF-8
        sb.append("key=").append(key);
        //进行MD5加密
        return DigestUtils.md5Hex(sb.toString().getBytes(StandardCharsets.UTF_8)).toUpperCase();
    }

    /**
     * 校验签名
     *
     * @param map 对象参数
     * @param key 签名密钥
     * @return
     */
    public static boolean verifySign(Map<String, Object> map, String key) {
        // 将API返回的数据根据用签名算法进行计算新的签名，用来跟API返回的签名进行比较
        String sign = map.get("sign").toString();
        String resultSign = createSign(map, key);
        if (sign != null && sign.toUpperCase().equals(resultSign)) {
            return true;
        }
        return false;
    }
}
