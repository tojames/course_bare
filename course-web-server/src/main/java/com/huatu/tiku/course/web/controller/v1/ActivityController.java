package com.huatu.tiku.course.web.controller.v1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.huatu.springboot.web.version.mapping.annotation.ApiVersion;
import com.huatu.tiku.common.bean.user.UserSession;
import com.huatu.tiku.course.service.v1.ActivityService;
import com.huatu.tiku.springboot.users.support.Token;

/**
 * 
 * @author zhangchong
 *
 */
@RestController
@RequestMapping("activity")
@ApiVersion("v1")
public class ActivityController {

	@Autowired
	private ActivityService activityService;

	/**
	 * 活动签到
	 * 
	 * @return 0 已经签到 1签到成功 -1 签到异常
	 */
	@PostMapping("/sign")
	public Object activitySign(@Token UserSession userSession, @RequestHeader(value = "terminal") Integer terminal,
			@RequestHeader(value = "cv") String cv) {
		return activityService.signGiveCoin(userSession.getUname());
	}

	/**
	 * 签到记录
	 * 
	 * @param userSession
	 * @param terminal
	 * @param cv
	 * @return
	 */
	@GetMapping("/list")
	public Object activitySignList(@Token UserSession userSession, @RequestHeader(value = "terminal") Integer terminal,
			@RequestHeader(value = "cv") String cv) {
		return activityService.signList(userSession.getUname());
	}

}
