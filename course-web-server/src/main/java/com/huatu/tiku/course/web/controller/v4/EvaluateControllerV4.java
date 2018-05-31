package com.huatu.tiku.course.web.controller.v4;

import com.huatu.common.ErrorResult;
import com.huatu.tiku.common.bean.user.UserSession;
import com.huatu.tiku.course.bean.NetSchoolResponse;
import com.huatu.tiku.course.netschool.api.v4.AppServiceV4;
import com.huatu.tiku.course.util.ResponseUtil;
import com.huatu.tiku.springboot.users.support.Token;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by lijun on 2018/5/18
 */
@RestController
@RequestMapping("/v4/evaluates")
public class EvaluateControllerV4 {

    @Autowired
    private AppServiceV4 appService;

    /**
     * 课件是否被用户评价
     *
     * @return
     */
    @GetMapping("/lession")
    public Object lessionEvaluate(
            @Token UserSession userSession,
            @RequestParam("lessionId") int lessionId
    ) {
        NetSchoolResponse netSchoolResponse = appService.lessionEvaluate(lessionId, userSession.getUname());
        return ResponseUtil.build(netSchoolResponse);
    }


    @GetMapping("/token")
    public Object lessionToken(
            @RequestParam(required = false) String bjyRoomId,
            @RequestParam(required = false) String bjySessionId,
            @RequestParam(required = false) String videoId) {
        if (StringUtils.isBlank(bjyRoomId) && StringUtils.isBlank(videoId)) {
            return ErrorResult.create(50000, "服务器内部错误,缺少参数");
        }
        NetSchoolResponse netSchoolResponse = appService.lessionToken(bjyRoomId, bjySessionId, videoId);
        return ResponseUtil.build(netSchoolResponse);
    }

    @GetMapping("/collectionClasses")
    public Object collectionClasses(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int pageSize,
            @RequestParam int collectionId
    ) {
        NetSchoolResponse netSchoolResponse = appService.collectionClasses(page, pageSize, collectionId);
        return ResponseUtil.build(netSchoolResponse);
    }
}
