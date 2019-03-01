package com.huatu.tiku.course.service.v1.impl.practice;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.huatu.common.utils.collection.HashMapBuilder;
import com.huatu.tiku.course.bean.practice.*;
import com.huatu.tiku.course.service.cache.CoursePracticeCacheKey;
import com.huatu.tiku.course.service.v1.practice.QuestionInfoService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by lijun on 2019/2/27
 */
@Component
@RequiredArgsConstructor
public class PracticeMetaComponent {

    private static final String QUESTION_TOTAL_TIME_KEY = "0";

    final RedisTemplate redisTemplate;
    final QuestionInfoService questionInfoService;

    /**
     * 构建用户统计 缓存信息
     */
    public void buildUserQuestionMeta(Integer userId, Long courseId, Long questionId, String answer, Integer time, Integer correct) {
        final HashOperations<String, String, PracticeUserQuestionMetaInfoBo> hashOperations = redisTemplate.opsForHash();
        String key = CoursePracticeCacheKey.userMetaKey(userId, courseId);
        final PracticeUserQuestionMetaInfoBo metaInfo = PracticeUserQuestionMetaInfoBo.builder()
                .time(time)
                .answer(answer)
                .correct(correct)
                .build();
        hashOperations.put(key, String.valueOf(questionId), metaInfo);
        redisTemplate.expire(key, CoursePracticeCacheKey.getDefaultKeyTTL(), CoursePracticeCacheKey.getDefaultTimeUnit());
    }

    /**
     * 获取用户在该房间的答题数据
     *
     * @param userId   用户ID
     * @param courseId 课程ID
     * @return
     */
    public PracticeRoomRankUserBo getPracticeRoomRankUser(Integer userId, Long courseId) {
        final HashOperations<String, String, PracticeUserQuestionMetaInfoBo> hashOperations = redisTemplate.opsForHash();
        String key = CoursePracticeCacheKey.userMetaKey(userId, courseId);
        final Map<String, PracticeUserQuestionMetaInfoBo> entries = hashOperations.entries(key);
        if (null == entries) {
            return null;
        }
        final Collection<PracticeUserQuestionMetaInfoBo> values = entries.values();
        Integer totalTime = values.stream().map(PracticeUserQuestionMetaInfoBo::getTime).reduce(0, (a, b) -> a + b);
        Integer totalScore = values.stream()
                .map(practiceUserQuestionMetaInfoBo -> practiceUserQuestionMetaInfoBo.getCorrect() == 1 ? 1 : 0)
                .reduce(0, (a, b) -> a + b);
        return PracticeRoomRankUserBo.builder()
                .id(userId)
                .totalScore(totalScore)
                .totalTime(totalTime)
                .build();
    }

    /**
     * 构建 房间统计信息
     */
    public void buildRoomRank(Long roomId, Long courseId, Integer userId, String userName, Long questionId, String answer, Integer time, Integer correct) {
        //先校验当前数据是否存在
        final PracticeRoomRankUserBo practiceRoomRankUserBo = getPracticeRoomRankUser(userId, courseId);
        if (null == practiceRoomRankUserBo) {
            buildUserQuestionMeta(userId, courseId, questionId, answer, time, correct);
        }
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        final String key = CoursePracticeCacheKey.roomRankKey(roomId);
        practiceRoomRankUserBo.setName(userName);
        //此处不能有统计信息，否则 zset 中会出现多个数据
        final HashMap<String, Object> map = HashMapBuilder.<String, Object>newBuilder()
                .put("id", practiceRoomRankUserBo.getId())
                .put("name", practiceRoomRankUserBo.getName())
                .build();
        zSetOperations.add(key, JSONObject.toJSONString(map), practiceRoomRankUserBo.buildRankInfo());
        redisTemplate.expire(key, CoursePracticeCacheKey.getDefaultKeyTTL(), CoursePracticeCacheKey.getDefaultTimeUnit());
    }

    /**
     * 获取统计信息
     */
    public List<PracticeRoomRankUserBo> getRoomRankInfo(Long roomId, Integer start, Integer end) {
        final ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();
        final String key = CoursePracticeCacheKey.roomRankKey(roomId);
        final Set<ZSetOperations.TypedTuple<String>> typedTupleSet = zSetOperations.rangeWithScores(key, start, end);

        final List<PracticeRoomRankUserBo> result = typedTupleSet.stream()
                .map(typedTuple -> {
                    int totalTime = typedTuple.getScore().intValue() % 1000;
                    int totalScore = (typedTuple.getScore().intValue() - totalTime) / 1000;
                    final JSONObject jsonObject = JSONObject.parseObject(typedTuple.getValue());
                    return PracticeRoomRankUserBo.builder()
                            .id(jsonObject.getInteger("id"))
                            .name(jsonObject.getString("name"))
                            .totalTime(totalTime)
                            .totalScore(totalScore < 0? 0 : totalScore)
                            .build();
                })
                .collect(Collectors.toList());
        return result;
    }

    /**
     * 构建试题统计信息
     * '0' 存储消耗总时间
     */
    public void buildQuestionMeta(Long roomId, Long questionId, String answer, Integer time) {
        if (StringUtils.isBlank(answer)) {
            return;
        }
        final HashOperations<String, String, Integer> hashOperations = redisTemplate.opsForHash();
        final String key = CoursePracticeCacheKey.questionMetaKey(roomId, questionId);
        hashOperations.increment(key, QUESTION_TOTAL_TIME_KEY, time);
        hashOperations.increment(key, answer, 1);
        redisTemplate.expire(key, CoursePracticeCacheKey.getDefaultKeyTTL(), CoursePracticeCacheKey.getDefaultTimeUnit());
    }

    /**
     * 获取试题统计信息
     */
    public QuestionMetaBo getQuestionMetaBo(Long roomId, Long questionId) {
        final HashOperations<String, String, Integer> hashOperations = redisTemplate.opsForHash();
        final String key = CoursePracticeCacheKey.questionMetaKey(roomId, questionId);
        final Integer totalTime = hashOperations.get(key, QUESTION_TOTAL_TIME_KEY);
        if (totalTime == 0) {
            return new QuestionMetaBo();
        }
        List<QuestionInfo> baseQuestionInfoList = questionInfoService.getBaseQuestionInfo(Lists.newArrayList(questionId));
        if (CollectionUtils.isEmpty(baseQuestionInfoList)) {
            return new QuestionMetaBo();
        }
        final int[] answerCountNum = new int[4];
        final Set<Map.Entry<String, Integer>> entrySet = hashOperations.entries(key).entrySet();
        entrySet.stream()
                .forEach(entry -> {
                    //只统计A、B、C、D 四个选项
                    IntStream.rangeClosed(1, 4).forEach(index -> {
                        if (entry.getKey().contains(String.valueOf(index))) {
                            answerCountNum[index - 1] += entry.getValue();
                        }
                    });
                });
        final Integer totalCount = entrySet.stream()
                .filter(entry -> !entry.getKey().equals(QUESTION_TOTAL_TIME_KEY))
                .map(Map.Entry::getValue)
                .reduce(0, (a, b) -> a + b);
        return QuestionMetaBo.builder()
                .id(baseQuestionInfoList.get(0).getId())
                .answer(baseQuestionInfoList.get(0).getAnswer())
                .avgTime(totalTime / totalCount)
                .count(totalCount)
                .percents(answerCountNum)
                .build();
    }

    /**
     * 获取学员统计信息
     */
    public StudentQuestionMetaBo getStudentQuestionMetaBo(Long roomId, Long questionId) {
        QuestionMetaBo questionMetaBo = getQuestionMetaBo(roomId, questionId);
        final StudentQuestionMetaBo studentQuestionMetaBo = new StudentQuestionMetaBo();
        BeanUtils.copyProperties(questionMetaBo, studentQuestionMetaBo);
        return studentQuestionMetaBo;
    }


    /**
     * 构建统计信息
     *
     * @param roomId     房间ID
     * @param userId     用户ID
     * @param courseId   课件ID
     * @param questionId 试题ID
     * @param answer     用户答案
     * @param time       用户答题耗时
     * @param correct    是否正确 1 正确，2 错误
     */
    public void buildMetaInfo(Integer userId, String userName, Long roomId, Long courseId, Long questionId, String answer, Integer time, Integer correct) {
        buildUserQuestionMeta(userId, courseId, questionId, answer, time, correct);
        buildQuestionMeta(roomId, questionId, answer, time);
        buildRoomRank(roomId, courseId, userId, userName, questionId, answer, time, correct);
    }
}
