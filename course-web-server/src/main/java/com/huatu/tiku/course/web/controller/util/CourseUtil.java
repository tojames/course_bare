package com.huatu.tiku.course.web.controller.util;

import com.alibaba.fastjson.JSONObject;
import com.huatu.common.spring.event.EventPublisher;
import com.huatu.common.utils.collection.HashMapBuilder;
import com.huatu.tiku.common.bean.user.UserSession;
import com.huatu.tiku.course.bean.NetSchoolResponse;
import com.huatu.tiku.course.hbase.api.v1.VideoServiceV1;
import com.huatu.tiku.course.util.ResponseUtil;
import com.huatu.tiku.course.util.ZTKResponseUtil;
import com.huatu.tiku.course.ztk.api.v1.paper.PracticeCardServiceV1;
import com.huatu.tiku.springboot.basic.reward.RewardAction;
import com.huatu.tiku.springboot.basic.reward.event.RewardActionEvent;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by lijun on 2018/6/25
 */
@Component
public class CourseUtil {

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private VideoServiceV1 videoServiceV1;

    @Autowired
    private PracticeCardServiceV1 practiceCardServiceV1;

    /**
     * 添加课程播放的时间-用以每日任务处理
     */
    public void pushPlayEvent(UserSession userSession, NetSchoolResponse netSchoolResponse, Object response) {
        //发布事件
        if (ResponseUtil.isSuccess(netSchoolResponse) && response instanceof Map && ((Map) response).containsKey("course")) {
            Object courseDetail = ((Map) response).get("course");
            if (courseDetail instanceof Map && ((Map) courseDetail).containsKey("free") && "1".equals(String.valueOf(((Map) courseDetail).get("free")))) {
                //免费课
                eventPublisher.publishEvent(RewardActionEvent.class,
                        this,
                        (event) -> event.setAction(RewardAction.ActionType.WATCH_FREE)
                                .setUname(userSession.getUname())
                                .setUid(userSession.getId())
                );
            } else {
                //收费课
                eventPublisher.publishEvent(RewardActionEvent.class,
                        this,
                        (event) -> event.setAction(RewardAction.ActionType.WATCH_PAY)
                                .setUname(userSession.getUname())
                                .setUid(userSession.getId())
                );
            }
        }
    }

    /**
     * 在播放列表添加播放进度
     *
     * @param response 播放列表信息
     */
    public void addStudyProcessIntoSecrInfo(Object response, final String token, final String cv, final int terminal) {
        if (null != response) {
            JSONObject result = (JSONObject) response;
            Object resultList = result.get("lession");
            if (null != resultList) {
                //批量接口查询
                List<HashMap> paramList = ((List<Map>) resultList).parallelStream()
                        .map(data -> {
                            HashMap params = HashMapBuilder.<String, Object>newBuilder()
                                    .put("rid", String.valueOf(data.get("rid")))
                                    .put("joinCode", data.get("JoinCode") == null ? "" : String.valueOf(data.get("JoinCode")))
                                    .put("roomId", data.get("bjyRoomId") == null ? "" : String.valueOf(data.get("bjyRoomId")))
                                    .put("sessionId", data.get("bjySessionId") == null ? "" : String.valueOf(data.get("bjySessionId")))
                                    .build();
                            params.put((data.get("hasTeacher") == null || String.valueOf(data.get("hasTeacher")).equals("0")) ? "videoIdWithoutTeacher" : "videoIdWithTeacher",
                                    data.get("bjyVideoId") == null ? "" : String.valueOf(data.get("bjyVideoId")));
                            return params;
                        })
                        .collect(Collectors.toList());

                long currentTimeMillis = System.currentTimeMillis();
                Object data = videoServiceV1.videoProcessDetailV1(token, terminal, cv, paramList);
                //log.info(" videoServiceV1 videoProcessDetailV1 ===> token = {},paramList = {}",token, JSON.toJSON(paramList));
                List<HashMap<String, Object>> hbaseDataList = (List<HashMap<String, Object>>) ((Map<String, Object>) data).get("data");
                //log.info(" videoServiceV1 videoProcessDetailV1 ===> result = {},time = {}",JSON.toJSON(hbaseDataList),System.currentTimeMillis() - currentTimeMillis);

                if (null != hbaseDataList) {
                    //组装进度数据
                    List<Map> list = ((List<Map>) resultList).parallelStream()
                            .map(lessionData -> {
                                //查询匹配的数据
                                Optional<HashMap<String, Object>> first = hbaseDataList.parallelStream()
                                        .filter(hBaseData -> String.valueOf(lessionData.get("rid")).equals(String.valueOf(hBaseData.get("rid"))))
                                        .findFirst();
                                //如果有匹配数据
                                if (first.isPresent()) {
                                    HashMap<String, Object> buildResult = first.get();
                                    if (null == buildResult || null == buildResult.get("wholeTime") || (int) buildResult.get("wholeTime") == 0) {
                                        lessionData.put("process", 0);
                                    } else {
                                        float process = Float.valueOf((int) buildResult.get("playTime"))
                                                / Float.valueOf((int) buildResult.get("wholeTime"));
                                        BigDecimal bg = new BigDecimal(process);
                                        double f1 = bg.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
                                        lessionData.put("process", f1 * 100);
                                    }
                                } else {
                                    lessionData.put("process", 0);
                                }
                                return lessionData;
                            })
                            .collect(Collectors.toList());
                    result.replace("result", list);
                } else {
                    //此处只会在 快速失败的情况下被调用
                    List<Map> list = ((List<Map>) resultList).parallelStream()
                            .map(lessionData -> {
                                lessionData.put("process", 0);
                                return lessionData;
                            })
                            .collect(Collectors.toList());
                    result.replace("result", list);
                }
            }
        }
    }

    /**
     * 课程大纲-售后-添加课后答题结果信息
     *
     * @param response 响应结果集
     * @param userId   用户ID
     */
    public void addExercisesCardInfo(LinkedHashMap response, long userId) {

        response.computeIfPresent("list", (key, value) -> {
                    List<HashMap<String, Object>> paramsList = ((List<Map>) value).stream()
                            .filter(map -> (null != MapUtils.getString(map, "type")
                                    && MapUtils.getString(map, "type").equals("2"))
                            )
                            .filter(map -> null != map.get("videoType") && null != map.get("coursewareId"))
                            .map(map -> {
                                HashMap<String, Object> build = HashMapBuilder.<String, Object>newBuilder()
                                        .put("courseType", MapUtils.getIntValue(map, "videoType", 0))
                                        .put("courseId", MapUtils.getIntValue(map, "coursewareId", 0))
                                        .build();
                                return build;
                            })
                            .collect(Collectors.toList());
                    //查询用户答题信息
                    Object courseExercisesCardInfo = practiceCardServiceV1.getCourseExercisesCardInfo(userId, paramsList);
                    Object build = ZTKResponseUtil.build(courseExercisesCardInfo);

                    Map<Object, Object> defaultMap = HashMapBuilder.newBuilder()
                            .put("status", 0)
                            .put("rcount", 0)
                            .put("wcount", 0)
                            .put("ucount", 0)
                            .build();
                    if (null != build && 0 != ((List<Map>) build).size()) {
                        //获取答题卡信息状态
                        Function<HashMap<String, Object>, Map> getCourse = (valueData) -> {
                            Optional<Map> first = ((List<Map>) build).stream()
                                    .filter(result -> null != result.get("courseId") && null != result.get("courseType"))
                                    .filter(result ->
                                            MapUtils.getString(result, "courseId").equals(MapUtils.getString(valueData, "coursewareId"))
                                                    && MapUtils.getString(result, "courseType").equals(MapUtils.getString(valueData, "videoType"))
                                    )
                                    .findFirst();
                            if (first.isPresent()) {
                                Map map = first.get();
                                map.remove("courseId");
                                map.remove("courseType");
                                return map;
                            } else {
                                return defaultMap;
                            }
                        };
                        List<Map> mapList = ((List<Map>) value).stream()
                                .map(valueData -> {
                                    Map answerCard = getCourse.apply((HashMap<String, Object>) valueData);
                                    valueData.put("answerCard", answerCard);
                                    return valueData;
                                })
                                .collect(Collectors.toList());
                        return mapList;
                    } else {
                        List<Map> mapList = ((List<Map>) value).stream()
                                .map(valueData -> {
                                    valueData.put("answerCard", defaultMap);
                                    return valueData;
                                })
                                .collect(Collectors.toList());
                        return mapList;
                    }
                }
        );
    }

}
