package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.cmn.client.DictFeignClient;
import com.atguigu.yygh.enums.DictEnum;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalServiceImpl implements HospitalService {
    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private DictFeignClient dictFeignClient;

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
            hospital.setStatus(0);
            hospitalRepository.save(hospital);
        }else{
            //新增
            hospital.setIsDeleted(0);
            hospital.setCreateTime(new Date());
            hospital.setUpdateTime(new Date());
            hospital.setStatus(0);
            hospitalRepository.insert(hospital);
        }
    }

    @Override
    public Hospital getHospByHoscode(String hoscode) {
        return hospitalRepository.getByHoscode(hoscode);
    }

    @Override
    public Page<Hospital> selectPage(Integer page, Integer limit,
                                     HospitalQueryVo hospitalQueryVo) {
        //1创建分页对象
        //1.1创建排序对象
        Sort sort = Sort.by(Sort.Direction.DESC,"createTime");
        //1.2创建分页对象
        Pageable pageable = PageRequest.of((page-1),limit,sort);
        //2创建查询条件模板
        //2.1模板构造器
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        //2.2封装查询条件
        Hospital hospital = new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo,hospital);
        //2.3创建模板
        Example<Hospital> example = Example.of(hospital,matcher);
        //3查询数据
        Page<Hospital> pageModel = hospitalRepository.findAll(example, pageable);
        //4 TODO 跨模块翻译字段
        pageModel.getContent().forEach(this::packHospital);

        return pageModel;
    }

    @Override
    public void updateStatus(String id, Integer status) {
        if(status == 0 || status == 1) {
            Hospital hospital = hospitalRepository.findById(id).get();
            hospital.setStatus(status);
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);
        }
    }
    /*
    *
    *展示医院的数据
    * */
    @Override
    public Map<String, Object> show(String id) {
        //通过ID拿到医院的数据
        //并且给他的数据进行替换
        Hospital hospital1 = this.packHospital(hospitalRepository.getById(id));
        //创建一个map集合
        Map<String, Object> map = new HashMap<>();
        BookingRule bookingRule = hospital1.getBookingRule();
        hospital1.setBookingRule("");
        map.put("bookingRule",bookingRule);
        map.put("hospital",hospital1);
        return map;
    }

    @Override
    public List<Hospital> getByHosnameLike(String hosname) {
        return hospitalRepository.getHospitalByHosnameLike(hosname);
    }

    @Override
    public Map<String, Object> findHospMapByHoscode(String hoscode) {
        Hospital byHoscode = this.packHospital(hospitalRepository.getByHoscode(hoscode));
        BookingRule bookingRule = byHoscode.getBookingRule();
        byHoscode.setBookingRule("");
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("bookingRule",bookingRule);
            hashMap.put("list",byHoscode);
        return hashMap;
    }

    @Override
    public String getHospName(String hoscode) {
        Hospital byHoscode = hospitalRepository.getByHoscode(hoscode);
        String hosname = byHoscode.getHosname();
        return hosname;
    }

    private Hospital packHospital(Hospital hospital) {
        //翻译省市区
        String provinceCode = hospital.getProvinceCode();
        String provinceString = dictFeignClient.getName(provinceCode);
        String cityString = dictFeignClient.getName(hospital.getCityCode());
        String districtString = dictFeignClient.getName(hospital.getDistrictCode());
        //翻译医院等级
        String hostypeString = dictFeignClient.getName(DictEnum.HOSTYPE.getDictCode(),hospital.getHostype());
        hospital.getParam().put("hostypeString", hostypeString);
        hospital.getParam().put("fullAddress", provinceString + cityString + districtString + hospital.getAddress());
        return hospital;
    }
}
