package com.atguigu.yygh.hosp.repository;

import com.atguigu.yygh.model.hosp.Schedule;
import lombok.Data;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface ScheduleRepository extends MongoRepository<Schedule,String> {
    Schedule getByHoscodeAndHosScheduleId(String hoscode, String hosScheduleId);

    List<Schedule> getByHoscodeAndDepcodeAndWorkDate(String hoscode, String depcode, Date workDate);
}
