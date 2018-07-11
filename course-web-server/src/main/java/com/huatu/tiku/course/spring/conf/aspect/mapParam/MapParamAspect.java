package com.huatu.tiku.course.spring.conf.aspect.mapParam;

import com.huatu.common.ErrorResult;
import com.huatu.common.exception.BizException;
import com.huatu.common.utils.collection.HashMapBuilder;
import com.huatu.tiku.common.bean.user.UserSession;
import com.huatu.tiku.springboot.users.service.UserSessionService;
import com.huatu.tiku.springboot.users.support.UserSessionHolder;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 确认在其他数据处理完成之后再进行
 * Created by lijun on 2018/7/6
 */
@Order(Ordered.HIGHEST_PRECEDENCE - 100)
@Component
@Aspect
public class MapParamAspect {

    @Autowired
    private UserSessionService userSessionService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 需要本地化的 head 请求参数
     */
    private static final String[] HEARD_PARAM = new String[]{"cv", "terminal", "appType"};

    @Pointcut("@annotation(com.huatu.tiku.course.spring.conf.aspect.mapParam.LocalMapParam)")
    private void mapParamMethod() {
    }

    /**
     * 进入方法之前 转换参数
     */
    @Before("mapParamMethod()")
    public void before(JoinPoint joinPoint) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        //1.build Heard 信息
        HashMapBuilder<String, Object> hashMapBuilder = HashMapBuilder.newBuilder();
        //1.1 处理token - 可能需要验证 token
        if (StringUtils.isNotBlank(request.getHeader("token"))) {
            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            //获取当前 token -> userName 方法
            LocalMapParam localMapParam = method.getAnnotation(LocalMapParam.class);
            if (localMapParam.needUserName()) {
                TokenType tokenType = localMapParam.tokenType();
                String userName = "";
                switch (tokenType) {
                    case ZTK:
                        userName = getZTKUserName(request.getHeader("token"));
                        break;
                    case IC:
                        userName = getICUserName(request.getHeader("token"));
                        break;
                }
                if (localMapParam.checkToken() && StringUtils.isBlank(userName)) {
                    throw new BizException(ErrorResult.create(5000000, "登录信息失效,请重新登录"));
                }
                if (StringUtils.isNotBlank(userName)) {
                    hashMapBuilder.put("userName", userName);
                }
            }
        }
        //1.2 处理其他的head 信息
        for (String headStr : HEARD_PARAM) {
            if (StringUtils.isNotBlank(request.getHeader(headStr))) {
                hashMapBuilder.put(headStr, request.getHeader(headStr));
            }
        }
        //2. build RequestBody
        Map<String, String[]> map = request.getParameterMap();
        if (null != map) {
            Function<String[], String> transParam = (param) -> Arrays.asList(param).stream().collect(Collectors.joining(","));
            for (String entry : map.keySet()) {
                hashMapBuilder.put(entry, transParam.apply(map.get(entry)));
            }
        }
        //3. build PathVariable
        NativeWebRequest nativeWebRequest = new ServletWebRequest(request);
        Map<String, String> pathParam = (Map<String, String>) nativeWebRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (null != pathParam) {
            for (String key : pathParam.keySet()) {
                //此处不需要做 notBlank 判断，在路径上的参数 必定不会null
                hashMapBuilder.put(key, pathParam.get(key));
            }
        }
        LocalMapParamHandler.set(hashMapBuilder.build());
    }

    /**
     * 完成之后手动释放
     */
    @AfterReturning(value = "mapParamMethod()")
    public void after() {
        LocalMapParamHandler.clean();
    }

    /**
     * 异常后手动释放
     */
    @AfterThrowing(value = "mapParamMethod()")
    public void exception() {
        LocalMapParamHandler.clean();
    }


    /**
     * 根据 token 获取用户信息 此处主要获取用户名称 需关联start-user
     *
     * @return 用户名称
     */
    private String getZTKUserName(String token) {
        //从当前线程存储获取，如果没有再去redis查找，减少访问次数
        UserSession userSession = UserSessionHolder.get();
        if (userSession == null) {
            userSession = userSessionService.getUserSession(token);
        }
        if (userSession != null) {
            return userSession.getUname();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取IC(面库用户名)
     *
     * @return 用户名称
     */
    private String getICUserName(String token) {
        RedisConnection connection = redisTemplate.getConnectionFactory().getConnection();
        try {
            Map<byte[], byte[]> map = connection.hGetAll(token.getBytes());
            if (null == map) {
                return StringUtils.EMPTY;
            }
            byte[] bytes = map.get("\"username\"".getBytes());
            if (null == bytes) {
                return StringUtils.EMPTY;
            }
            String userName = new String(bytes);
            if (StringUtils.isNotBlank(userName)) {
                return userName.substring(0, userName.length() - 1);
            }
            return userName;
        } finally {
            connection.close();
        }
    }
}