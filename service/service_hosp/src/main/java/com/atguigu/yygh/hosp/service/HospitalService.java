package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface HospitalService {
    void saveHospital(Map<String, Object> stringObjectMap);

    Hospital getHospByHoscode(String hoscode);

    Page<Hospital> selectPage(Integer page, Integer limit, com.atguigu.yygh.vo.hosp.HospitalQueryVo hospitalQueryVo);

    void updateStatus(String id, Integer status);

    Map<String, Object> show(String id);
}
