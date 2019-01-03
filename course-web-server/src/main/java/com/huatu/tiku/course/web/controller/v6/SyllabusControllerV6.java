package com.huatu.tiku.course.web.controller.v6;

import com.huatu.springboot.web.version.mapping.annotation.ApiVersion;
import com.huatu.tiku.common.bean.user.UserSession;
import com.huatu.tiku.course.bean.NetSchoolResponse;
import com.huatu.tiku.course.netschool.api.v6.SyllabusServiceV6;
import com.huatu.tiku.course.spring.conf.aspect.mapParam.LocalMapParam;
import com.huatu.tiku.course.spring.conf.aspect.mapParam.LocalMapParamHandler;
import com.huatu.tiku.course.util.ResponseUtil;
import com.huatu.tiku.course.web.controller.util.CourseUtil;
import com.huatu.tiku.springboot.users.support.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 描述：
 *
 * @author biguodong
 * Create time 2019-01-03 下午4:11
 **/

@Slf4j
@RestController
@RequestMapping("/syllabus")
@ApiVersion("v6")
public class SyllabusControllerV6 {


    @Autowired
    private SyllabusServiceV6 syllabusService;

    @Autowired
    private CourseUtil courseUtil;

    /**
     * 课程大纲课程列表 7.1.1
     * @param cv
     * @param terminal
     * @param netClassId
     * @param stageNodeId
     * @return
     */
    @LocalMapParam
    @GetMapping("/{netClassId}/syllabusClasses")
    public Object syllabusClasses(@RequestHeader(value = "cv") String cv,
                                  @RequestHeader(value = "terminal") int terminal,
                                  @PathVariable(value = "netClassId") int netClassId,
                                  @RequestParam(defaultValue = "0") int stageNodeId) {


        Map<String,Object> params = LocalMapParamHandler.get();
        NetSchoolResponse netSchoolResponse =  syllabusService.syllabusClasses(params);
        return ResponseUtil.build(netSchoolResponse);
    }


    /**
     * 售后大纲老师列表（7.1.1）
     */
    @LocalMapParam
    @GetMapping("{netClassId}/syllabusTeachers")
    public Object syllabusTeachers(@RequestHeader(value = "terminal") int terminal,
                                   @PathVariable(value = "netClassId") int netClassId,
                                   @RequestParam(defaultValue = "0") int classNodeId,
                                   @RequestParam(defaultValue = "0") int stageNodeId) {
        HashMap<String, Object> map = LocalMapParamHandler.get();
        return ResponseUtil.build(syllabusService.syllabusTeachers(map));
    }


    /**
     * 大纲 售后
     */
    @LocalMapParam(checkToken = true)
    @GetMapping("{netClassId}/buyAfterSyllabus")
    public Object buyAfterSyllabus(
            @Token UserSession userSession,
            @RequestHeader(value = "cv") String cv,
            @PathVariable int netClassId,
            @RequestParam(defaultValue = "") String classId,
            @RequestParam(defaultValue = "") String classNodeId,
            @RequestParam(defaultValue = "") String teacherId,
            @RequestParam(defaultValue = "0") int coursewareNodeId,
            @RequestParam(defaultValue = "1") int position,
            @RequestParam(defaultValue = "0") int nextClassNodeId,
            @RequestParam(defaultValue = "0") int stageNodeId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        HashMap<String, Object> map = LocalMapParamHandler.get();
        Object response = ResponseUtil.build(syllabusService.buyAfterSyllabus(map));
        //添加答题信息
        courseUtil.addExercisesCardInfo((LinkedHashMap) response, userSession.getId());
        return response;
    }
}