package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.Map;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;

    @Override
    public void save(Map<String, Object> paramMap) {
        //1转化参数
        String paramJSONString = JSONObject.toJSONString(paramMap);
        Schedule schedule = JSONObject.parseObject(paramJSONString, Schedule.class);

        //2查询排班信息
        Schedule targetSchedule =
                scheduleRepository.getByHoscodeAndHosScheduleId(
                        schedule.getHoscode(), schedule.getHosScheduleId()
                );
        if (targetSchedule != null) {
            //3修改
            schedule.setId(targetSchedule.getId());
            schedule.setUpdateTime(new Date());
            scheduleRepository.save(schedule);
        } else {
            //4新增
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            scheduleRepository.save(schedule);
        }
    }

    /*
     *分页和条件查询的返回排班表
     * */
    @Override
    public Page<Schedule> scheduleList(int limit, int page, Schedule schedule) {
        Sort sort = Sort.by(Sort.Direction.DESC, "creatTime");
        Pageable pageable = PageRequest.of((page - 1), limit, sort);

        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Example<Schedule> example = Example.of(schedule, exampleMatcher);
        Page<Schedule> schedulePage = scheduleRepository.findAll(example, pageable);
        return schedulePage;
    }

    @Override
    public void scheduleRemove(Map<String, Object> stringObjectMap) {
        Schedule schedule = scheduleRepository.getByHoscodeAndHosScheduleId(
                (String) stringObjectMap.get("hoscode"),
                (String) stringObjectMap.get("hosScheduleId")
        );
        if (schedule != null) {
            scheduleRepository.delete(schedule);
        }
    }
}
