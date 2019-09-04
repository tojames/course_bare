package com.huatu.tiku.course.mq.listeners;

import com.huatu.tiku.course.consts.RabbitMqConstants;
import com.huatu.tiku.course.netschool.api.v6.UserCourseServiceV6;
import com.huatu.tiku.course.service.manager.CourseExercisesProcessLogManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 描述：
 *
 * @author biguodong
 * Create time 2019-03-22 10:31 PM
 **/

@Slf4j
@Component
public class CourseExerciseEssayLogListener {

    @Autowired
    private CourseExercisesProcessLogManager courseExercisesProcessLogManager;

    @Autowired
    private UserCourseServiceV6 userCourseService;

    @RabbitListener(queues = RabbitMqConstants.COURSE_LIVE_REPORT_LOG)
    public void onMessage(String message){

    }
}