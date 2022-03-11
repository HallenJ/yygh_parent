package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.handler.ZDYException;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


import java.sql.Array;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private HospitalService hospitalService;
    @Autowired
    private DepartmentService departmentService;

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
        baseMap.put("hosname", hosname);
        map.put("baseMap", baseMap);
        map.put("total", total);
        map.put("mappedResults", mappedResults);
        return map;
    }

    /*
     *
     *获取医院详细排班信息
     * */
    @Override
    public List<Schedule> getScheduleDetail(String hoscode, String depcode, String workDate) {
        Date date = new DateTime(workDate).toDate();
        List<Schedule> scheduleList = scheduleRepository.getByHoscodeAndDepcodeAndWorkDate(hoscode, depcode, date);
        if (scheduleList == null) {
            throw new ZDYException(2000, "出错啦");
        }
        return scheduleList;
    }

    @Override
    public Map<String, Object> getBookingSchedule(Integer page, Integer limit, String hoscode, String depcode) {
        HashMap<String, Object> result = new HashMap<>();
        //1根据hoscode查询医院信息，取出预约规则
        Hospital hospital = hospitalService.getHospByHoscode(hoscode);
        if (hospital == null) {
            throw new ZDYException(20001, "医院信息有误");
        }
        BookingRule bookingRule = hospital.getBookingRule();
        //2根据预约规则、分页信息查询可以预约的日期集合的分页对象（IPage<Date>）
        IPage<Date> iPage = this.getDateListPage(page, limit, bookingRule);
        List<Date> dateList = iPage.getRecords();
        //3参考后台接口，实现聚合查询排班信息
        //3.1创建筛选条件
        Criteria criteria = Criteria.where("hoscode").is(hoscode)
                .and("depcode").is(depcode).and("workDate").in(dateList);
        //3.2创建聚合查询对象
        Aggregation agg = Aggregation.newAggregation(
                //3.3设置筛选条件
                Aggregation.match(criteria),
                //3.4设置分组聚合信息
                Aggregation.group("workDate")
                        .first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber")
        );
        //3.3聚合查询
        AggregationResults<BookingScheduleRuleVo> aggregate =
                mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        List<BookingScheduleRuleVo> scheduleVoList = aggregate.getMappedResults();
        //3.4集合类型转化，scheduleVoList转化Map  k：workDate v:BookingScheduleRuleVo
        Map<Date, BookingScheduleRuleVo> scheduleVoMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(scheduleVoList)) {
            scheduleVoMap = scheduleVoList.stream().collect(Collectors.toMap(
                    BookingScheduleRuleVo::getWorkDate,
                    BookingScheduleRuleVo -> BookingScheduleRuleVo
            ));
        }
        //4把步骤2、3数据整合 (1) dateList (2)scheduleVoMap
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList = new ArrayList<>();
        for (int i = 0, let = dateList.size(); i < let; i++) {
            //4.1从dateList取出每一天日期date
            Date date = dateList.get(i);
            //4.2拿date从scheduleVoMap取出对应排班统计数据
            BookingScheduleRuleVo bookingScheduleRuleVo = scheduleVoMap.get(date);
            //4.3如果排班统计数据为空，初始化排班统计数据
            if (bookingScheduleRuleVo == null) {
                bookingScheduleRuleVo = new BookingScheduleRuleVo();
                bookingScheduleRuleVo.setDocCount(0);
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            //4.4 设置排班日期date
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //4.5 根据date换算周几
            String dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
            //4.6 根据时间判断统计数据状态值
            //状态 0：正常 1：即将放号 -1：当天已停止挂号"
            //最后一页，最后一条记录，状态值为即将放号
            if (i == let - 1 && page == iPage.getPages()) {
                bookingScheduleRuleVo.setStatus(1);
            } else {
                bookingScheduleRuleVo.setStatus(0);
            }

            //第一页第一条记录，判断是否过停止挂号时间
            if (i == 0 && page == 1) {
                DateTime stopDateTime = this.getDateTime(new Date(), bookingRule.getStopTime());
                if (stopDateTime.isBeforeNow()) {
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }

            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }
        //5封装数据，返回
        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHospName(hoscode));
        //科室
        Department department = departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
        //月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
        //放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
        //停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);
        return result;

    }

    //根据排班id获取排班详情
    //实现方法：根据id获取排班
    @Override
    public Schedule findScheduleById(String id) {
        Schedule schedule = scheduleRepository.findById(id).get();
        return this.packageSchedule(schedule);
    }

    //根据排班id获取预约下单数据
    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {
        //1 根据scheduleId查询排班信息
        Schedule schedule = scheduleRepository.findById(scheduleId).get();
        if(schedule==null){
            throw  new ZDYException(20001,"排班信息有误");
        }
        //2 根据排班信息中的hoscode，查询医院信息
        Hospital hospital = hospitalService.getHospByHoscode(schedule.getHoscode());
        if(hospital==null){
            throw  new ZDYException(20001,"医院信息有误");
        }
        //3 获取规则信息
        BookingRule bookingRule = hospital.getBookingRule();
        if(bookingRule==null){
            throw  new ZDYException(20001,"规则信息有误");
        }
        //4 封装基础数据
        ScheduleOrderVo scheduleOrderVo = new ScheduleOrderVo();
        scheduleOrderVo.setHoscode(schedule.getHoscode());
        scheduleOrderVo.setHosname(hospital.getHosname());
        scheduleOrderVo.setDepcode(schedule.getDepcode());
        scheduleOrderVo.setDepname(departmentService.getDepartment(schedule.getHoscode(), schedule.getDepcode()).getDepname());
        scheduleOrderVo.setHosScheduleId(schedule.getHosScheduleId());
        scheduleOrderVo.setAvailableNumber(schedule.getAvailableNumber());
        scheduleOrderVo.setTitle(schedule.getTitle());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());
        scheduleOrderVo.setAmount(schedule.getAmount());
        //5 封装规则计算出的时间数据
        //5.1退号时间date  (退号截止天+退号截止时间点)
        DateTime quitDate = new DateTime(schedule.getWorkDate()).plusDays(bookingRule.getQuitDay());
        DateTime quitDateTime = this.getDateTime(quitDate.toDate(),bookingRule.getQuitTime());
        scheduleOrderVo.setQuitTime(quitDateTime.toDate());
        //5.2 挂号开始时间
        DateTime startDateTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        scheduleOrderVo.setStartTime(startDateTime.toDate());
        //5.3预约截止时间
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate(), bookingRule.getStopTime());
        scheduleOrderVo.setEndTime(endTime.toDate());
        //当天停止挂号时间
        DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
        scheduleOrderVo.setStopTime(stopTime.toDate());
        return scheduleOrderVo;
    }

    @Override
    public void update(Schedule schedule) {
        scheduleRepository.save(schedule);
    }

    @Override
    public Schedule getScheduleByInfo(String hoscode, String hosScheduleId) {
        Schedule schedule =
                scheduleRepository.getByHoscodeAndHosScheduleId(hoscode,hosScheduleId);
        return schedule;
    }

    //封装排班详情其他值 医院名称、科室名称、日期对应星期
    private Schedule packageSchedule(Schedule schedule) {
        //设置医院名称
        schedule.getParam().put("hosname",hospitalService.getHospName(schedule.getHoscode()));
        //设置科室名称
        schedule.getParam().put("depname",
                departmentService.getDepartmentName(schedule.getHoscode(),schedule.getDepcode()));
        //设置日期对应星期
        schedule.getParam().put("dayOfWeek",this.getDayOfWeek(new DateTime(schedule.getWorkDate())));
        return schedule;
    }


    //查询需要展示的医院周期的page对象
    private IPage<Date> getDateListPage(
            Integer page, Integer limit, BookingRule bookingRule) {
        //1获取开始挂号时间（当前系统日期+放号时间）
        DateTime dateTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        //2取出预约周期，判断如果过了放号时间，周期+1
        Integer cycle = bookingRule.getCycle();
        if (dateTime.isBeforeNow()) cycle += 1;
        //3根据周期推算出可以挂号的日期集合List<Date>
        ArrayList<Date> canList = new ArrayList<>();
        for (int i = 0; i < cycle; i++) {
            Date date = new DateTime().plusDays(i).toDate();
            canList.add(date);
        }
        //4准备分页参数
        int start = (page - 1) * limit;
        int end = (page - 1) * limit + limit;
        if (end > canList.size()) end = canList.size();
        //5获取分页的日期集合List<Date>
        ArrayList<Date> returnList = new ArrayList<>();
        for (int i = 0; i < end; i++) {
            returnList.add(canList.get(i));
        }
        //6把数据封装到ipage里
        IPage<Date> iPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit, canList.size());
        iPage.setRecords(returnList);
        return iPage;


    }

    //将预约的开始放号时间 和 当前日期的年月日拼接 并返回dateTime对象
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date)
                .toString("yyyy-MM-dd") + " " + timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
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
