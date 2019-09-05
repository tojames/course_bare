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
public class EssayAnswerCardInfo extends AnswerCardInfo{

    /* 学员所得分数 */
    private double examScore;
    /*  试题总分 */
    private double score;

    public EssayAnswerCardInfo(double examScore, double score) {
        this.examScore = examScore;
        this.score = score;
    }

    public EssayAnswerCardInfo(int status, int wcount, int ucount, int rcount, int qcount, long id, double examScore, double score) {
        super(status, wcount, ucount, rcount, qcount, id);
        this.examScore = examScore;
        this.score = score;
    }
}
