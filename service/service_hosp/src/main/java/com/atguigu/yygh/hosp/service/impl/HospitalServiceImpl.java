package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class HospitalServiceImpl implements HospitalService {
    @Autowired
    private HospitalRepository hospitalRepository;

    @Override
    public void saveHospital(Map<String, Object> stringObjectMap) {
        String s = JSONObject.toJSONString(stringObjectMap);
        Hospital hospital = JSONObject.parseObject(s, Hospital.class);
        Hospital hospital1 =  hospitalRepository.getByHoscode(hospital.getHoscode());
        if(hospital1 !=null){
            //更新
            hospital.setIsDeleted(hospital1.getIsDeleted());
            hospital.setCreateTime(hospital1.getCreateTime());
            hospital.setUpdateTime(new Date());
            hospital.setId(hospital1.getId());
            Hospital save = hospitalRepository.save(hospital);
        }else{
            //新增
            hospital.setIsDeleted(0);
            hospital.setCreateTime(new Date());
            hospital.setUpdateTime(new Date());
            hospitalRepository.insert(hospital);
        }
    }

    @Override
    public Hospital getHospByHoscode(String hoscode) {
        Hospital byHoscode = hospitalRepository.getByHoscode(hoscode);
        return byHoscode;
    }
}
