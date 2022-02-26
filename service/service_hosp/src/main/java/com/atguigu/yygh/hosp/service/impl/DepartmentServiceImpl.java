package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;

    //存储部门信息
    @Override
    public void saveDepartment(Map<String, String[]> parameterMap) {
        String s = JSONObject.toJSONString(parameterMap);
        Department department = JSONObject.parseObject(s, Department.class);
        Department targetDepartment = departmentRepository.findByHoscodeAndDepcode(department.getHoscode(), department.getDepcode());
        if (targetDepartment == null) {
            //新增
            department.setCreateTime(new Date());
            department.setUpdateTime(new Date());
            department.setIsDeleted(0);
            departmentRepository.save(department);
        } else {
            //更新
            department.setUpdateTime(new Date());
            department.setIsDeleted(targetDepartment.getIsDeleted());
            departmentRepository.save(department);
        }
    }

    //带条件查询分页部门
    @Override
    public Page<Department> departmentList(int limit, int page, HospitalQueryVo hospitalQueryVo) {
        Sort sort = Sort.by(Sort.Direction.DESC, "creatTime");
        Pageable pageable = PageRequest.of((page - 1), limit, sort);
        ExampleMatcher exampleMatcher = ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        Department department = new Department();
        BeanUtils.copyProperties(hospitalQueryVo, department);
        Example example = Example.of(department, exampleMatcher);
        Page<Department> page1 = departmentRepository.findAll(example, pageable);
        return page1;
    }

    //部门删除
    @Override
    public void deleteDept(String hoscode, String depcode) {
        Department department = departmentRepository.getByHoscodeAndDepcode(hoscode, depcode);
        if (department != null) {
            departmentRepository.delete(department);
        }
    }
}
