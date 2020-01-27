package com.miaoshaproject.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

/**
 * @Author: crowsjian
 * @Date: 2019/12/14 13:25
 */
// 当Spring容器内没有tomcatEmbeddedServletContainerFactory这个bean时，会把此bean加载到spring容器
/*@Component*/
/*public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        // 使用工厂类提供的对象定制我们的tomcat connecter
        ((TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11Nio2Protocol protocol = (Http11Nio2Protocol)connector.getProtocolHandler();
                // 定制化keppAliveTimeOut, 30秒无请求断开连接
                protocol.setKeepAliveTimeout(30000);
                //当客户端发送请求超过1000个断开请求
                protocol.setMaxKeepAliveRequests(10000);
            }
        });
    }
}*/
