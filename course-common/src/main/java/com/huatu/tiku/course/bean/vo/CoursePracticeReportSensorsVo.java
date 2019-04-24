package com.huatu.tiku.course.bean.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 直播随堂练上报神策vo
 * 
 * @author zhangchong
 *
 */
@NoArgsConstructor
@Setter
@Getter
@Builder
@AllArgsConstructor
public class CoursePracticeReportSensorsVo {
	private Long roomId;
	private Long coursewareId;
	private Long questionId;
	//做对题目数
	private Integer rcount;
	//已经做题目数
	private Integer docount;
	//总题数
	private Integer qcount;
	//总用时
	private Integer times;
	private Boolean isFinish;
	
	private Integer userId;

}
