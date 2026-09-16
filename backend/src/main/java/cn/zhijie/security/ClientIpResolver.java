package cn.zhijie.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final List<IpAddressMatcher> proxies;

    public ClientIpResolver(@Value("${app.trusted-proxies:}") String trustedProxies) {
        proxies = Arrays.stream(trustedProxies.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(IpAddressMatcher::new)
            .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (proxies.stream().anyMatch(proxy -> proxy.matches(remote))) {
            // 单层受信任代理必须覆盖此头；不接受地址列表或主机名。
            String forwarded = request.getHeader("X-Real-IP");
            if (
                forwarded != null &&
                forwarded.length() <= 45 &&
                forwarded.matches("[0-9a-fA-F:.]+") &&
                (forwarded.contains(":") || forwarded.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}"))
            ) {
                try {
                    return InetAddress.getByName(forwarded).getHostAddress();
                } catch (java.net.UnknownHostException ignored) {
                    /* 非法来源回退到连接地址。 */
                }
            }
        }
        return remote;
    }
}
