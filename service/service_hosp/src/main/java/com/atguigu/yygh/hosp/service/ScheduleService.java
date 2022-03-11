package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ScheduleService {
    void save(Map<String, Object> stringObjectMap);

    Page<Schedule> scheduleList(int limit, int page, Schedule schedule);

    void scheduleRemove(Map<String, Object> stringObjectMap);

    Map<String,Object> getRuleSchedule(long page, long limit, String hoscode, String depcode);

    List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate);

    Map<String, Object> getBookingSchedule(Integer page, Integer limit, String hoscode, String depcode);

    Schedule findScheduleById(String id);

    ScheduleOrderVo getScheduleOrderVo(String scheduleId);

    void update(Schedule schedule);

    Schedule getScheduleByInfo(String hoscode, String hosScheduleId);
}
