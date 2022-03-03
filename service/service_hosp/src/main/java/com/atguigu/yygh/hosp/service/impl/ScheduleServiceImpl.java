package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.ZDYException;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;


import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private HospitalService hospitalService;

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

    @Override
    public Map<String, Object> getRuleSchedule(long page, long limit, String hoscode, String depcode) {
        Map<String, Object> map = new HashMap<>();
        Criteria criteria = Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.
                        group("workDate").
                        first("workDate").
                        as("workDate").
                        //统计号源信息
                                count().
                        as("docCount").
                        sum("reservedNumber").
                        as("reservedNumber").
                        sum("availableNumber").
                        as("availableNumber"),
                Aggregation.sort(Sort.Direction.ASC, "workDate"),
                Aggregation.skip((page - 1) * limit),
                Aggregation.limit(limit)
        );
        AggregationResults<BookingScheduleRuleVo> aggregate =
                mongoTemplate.
                        aggregate(
                                aggregation,
                                Schedule.class,
                                BookingScheduleRuleVo.class
                        );
        List<BookingScheduleRuleVo> mappedResults = aggregate.getMappedResults();
        Aggregation aggregation1 = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
        );
        AggregationResults<BookingScheduleRuleVo> aggregate1 = mongoTemplate.
                aggregate(
                        aggregation1,
                        Schedule.class,
                BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> mappedResults1 = aggregate1.getMappedResults();
        for (BookingScheduleRuleVo result : mappedResults) {
            Date workDate = result.getWorkDate();
            DateTime dateTime = new DateTime(workDate);
            String dayOfWeek = this.getDayOfWeek(dateTime);
            result.setDayOfWeek(dayOfWeek);
        }
        Hospital hospByHoscode = hospitalService.getHospByHoscode(hoscode);
        Map<String, Object> baseMap = new HashMap<>();
        String hosname = hospByHoscode.getHosname();
        int total = mappedResults1.size();
        baseMap.put("hosname",hosname);
        map.put("baseMap",baseMap);
        map.put("total",total);
        map.put("mappedResults",mappedResults);
        return map;
    }
    /*
    *
    *获取医院详细排班信息
    * */
    @Override
    public List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate) {
        Date date = new DateTime(workDate).toDate();
        List<Schedule> scheduleList= scheduleRepository.getByHoscodeAndDepcodeAndWorkDate(hoscode,depcode,date);
        if(scheduleList==null) {
            throw new ZDYException(2000, "出错啦");
        }
        return scheduleList;
    }


    /**
     * 根据日期获取周几数据
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }

}
