//package com.hz.web.config;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//import org.springframework.web.context.request.async.TimeoutCallableProcessingInterceptor;
//import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import java.util.Objects;
//
///**
// * @author saber
// */
//
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//    Logger logger = LoggerFactory.getLogger(WebMvcConfig.class);
//
//    private static final String FILTER_PATH = "/planning/**";
//
//    @Autowired
//    Environment environment;
//
//    @Autowired
//    private WebInterceptor webInterceptor;
//
//    @Value("${myProject.tomcatProject.projectName}")
//    private String projectName;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        String go_path = "/planning/user/login";
//        String go_path_admin = "/planning/user/loginMustAdmin";
//        String tomcat_go_path = "/" + projectName + go_path;
//        String tomcat_go_path_admin = "/" + projectName + go_path_admin;
//
//        //添加要拦截的url                                拦截的路径                                    放行的路径
//        registry.addInterceptor(webInterceptor).addPathPatterns(FILTER_PATH).excludePathPatterns(go_path, go_path_admin, tomcat_go_path, tomcat_go_path_admin);
//    }
//
//    @Override
//    public void configureAsyncSupport(final AsyncSupportConfigurer configurer) {
//        // 超时，单位毫秒
//        String requestTimeOut = environment.getProperty("myProject.server.requestTimeOut");
//        logger.info("超时时间设置为: {}毫秒", requestTimeOut);
//        configurer.setDefaultTimeout(Integer.parseInt(Objects.requireNonNull(requestTimeOut)));
//        configurer.registerCallableInterceptors(timeoutInterceptor());
//    }
//
//    @Bean
//    public TimeoutCallableProcessingInterceptor timeoutInterceptor() {
//        return new TimeoutCallableProcessingInterceptor();
//    }
//
//}
