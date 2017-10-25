package com.huatu.tiku.course.netschool.api.fall;

import com.huatu.tiku.course.bean.NetSchoolResponse;
import com.huatu.tiku.course.netschool.api.v3.PromoteCoreServiceV3;
import com.huatu.tiku.course.util.CourseCacheKey;
import com.netflix.hystrix.HystrixCommand;
import feign.hystrix.Fallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author hanchao
 * @date 2017/10/18 10:27
 */
@Component
@Slf4j
public class PromoteCoreServiceV3FallbackFactory implements Fallback<PromoteCoreServiceV3> {
    @Resource(name = "redisTemplate")
    private ListOperations listOperations;
    @Override
    public PromoteCoreServiceV3 create(Throwable throwable, HystrixCommand command) {
        return new PromoteCoreServiceV3() {
            @Override
            public NetSchoolResponse getPrevInfo(String p) {
                return NetSchoolResponse.DEFAULT_ERROR;
            }

            @Override
            public NetSchoolResponse getOrderDetail(String p) {
                return NetSchoolResponse.DEFAULT_ERROR;
            }

            @Override
            public NetSchoolResponse createOrder(String p) {
                //断路器打开状态，做不同的业务处理逻辑
                /*if(HystrixCircuitBreaker.Factory.getInstance(HystrixCommandKey.Factory.asKey("OrderServiceV3#createOrder(String)")).isOpen()){
                }*/
                //如果断路器当前是打开的，则将订单信息入队
                if(command.isCircuitBreakerOpen()){
                    log.info(">>> order circuitbreaker is open， push the request to queue...");
                    listOperations.leftPush(CourseCacheKey.ORDERS_QUEUE,p);
                }
                return NetSchoolResponse.DEFAULT_ERROR;
            }

            @Override
            public NetSchoolResponse payOrder(String p) {
                return NetSchoolResponse.DEFAULT_ERROR;
            }
        };
    }
}