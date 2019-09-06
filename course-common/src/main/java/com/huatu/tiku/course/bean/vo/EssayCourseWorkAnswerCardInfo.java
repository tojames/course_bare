package com.huatu.tiku.course.bean.vo;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 描述：申论课后作业信息
 *
 * @author biguodong
 * Create time 2019-08-30 2:54 PM
 **/


@Getter
@Setter
@NoArgsConstructor
public class EssayCourseWorkAnswerCardInfo extends EssayAnswerCardInfo{

    private Integer areaId;
    /* 地区name */
    private String areaName;
    /* 第几次作答 */
    private int correctNum;
    /* 单题组id */
    private long similarId;
    /* 单题 type 0 单题 1 试卷 2 议论文 */
    private int questionType;
    /* 单题 id */
    private Long questionBaseId;
    /* 套卷 id */
    private Long paperId;

    public EssayCourseWorkAnswerCardInfo(Integer areaId, String areaName, int correctNum, long similarId, int questionType, Long questionBaseId, Long paperId) {
        this.areaId = areaId;
        this.areaName = areaName;
        this.correctNum = correctNum;
        this.similarId = similarId;
        this.questionType = questionType;
        this.questionBaseId = questionBaseId;
        this.paperId = paperId;
    }

    public EssayCourseWorkAnswerCardInfo(double examScore, double score, Integer areaId, String areaName, int correctNum, long similarId, int questionType, Long questionBaseId, Long paperId) {
        super(examScore, score);
        this.areaId = areaId;
        this.areaName = areaName;
        this.correctNum = correctNum;
        this.similarId = similarId;
        this.questionType = questionType;
        this.questionBaseId = questionBaseId;
        this.paperId = paperId;
    }

    public EssayCourseWorkAnswerCardInfo(int status, int wcount, int ucount, int rcount, int qcount, long id, double examScore, double score, Integer areaId, String areaName, int correctNum, long similarId, int questionType, Long questionBaseId, Long paperId) {
        super(status, wcount, ucount, rcount, qcount, id, examScore, score);
        this.areaId = areaId;
        this.areaName = areaName;
        this.correctNum = correctNum;
        this.similarId = similarId;
        this.questionType = questionType;
        this.questionBaseId = questionBaseId;
        this.paperId = paperId;
    }
}
