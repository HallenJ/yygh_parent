package com.atguigu.yygh.hosp.service.impl;

import com.atguigu.yygh.hosp.mapper.HospitalSetMapper;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class HospitalSetServiceImpl extends ServiceImpl<HospitalSetMapper, HospitalSet> implements HospitalSetService {
    @Override
    public String getSignKey(String hoscode) {
        System.out.println("hoscode = " + hoscode);
        QueryWrapper<HospitalSet> hospitalQueryWrapper = new QueryWrapper<>();
        hospitalQueryWrapper.eq("hoscode",hoscode);
        HospitalSet hospitalSet = baseMapper.selectOne(hospitalQueryWrapper);
        String signKey = hospitalSet.getSignKey();
        return signKey;
    }
}
