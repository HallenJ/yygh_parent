package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Schedule;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface ScheduleService {
    void save(Map<String, Object> stringObjectMap);

    Page<Schedule> scheduleList(int limit, int page, Schedule schedule);

    void scheduleRemove(Map<String, Object> stringObjectMap);
}
