package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;

    //存储部门信息
    @Override
    public void saveDepartment(Map<String, Object> parameterMap) {
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
            departmentRepository.deleteById(department.getId());
        }
    }

    //实现所有部门树形查询
    @Override
    public List<DepartmentVo> getDeptList(String hoscode) {
        //创建一个List来进行返回
        List<DepartmentVo> depList = new ArrayList<>();
        //根据hoscode进行查询全部 在java中进行分类
        List<Department> allDepartmentList = departmentRepository.getByHoscode(hoscode);
        //对所拿到的list集合进行抽取
        Map<String, List<Department>> collect = allDepartmentList
                .stream()
                .collect(Collectors.groupingBy(Department::getBigcode));
        for (Map.Entry<String, List<Department>> listEntry : collect.entrySet()) {
            //创建一个对象去接收bigDep
            System.out.println("listEntry = " + listEntry);
            DepartmentVo departmentVo = new DepartmentVo();
            departmentVo.setDepname(listEntry.getValue().get(0).getBigname());
            departmentVo.setDepcode(listEntry.getKey());
            List<Department> value = listEntry.getValue();
            List<DepartmentVo> departmentVoList = new ArrayList<>();
            for (Department department : value) {
                String depcode = department.getDepcode();
                String depname = department.getDepname();
                DepartmentVo departmentVo1 = new DepartmentVo();
                departmentVo1.setDepcode(depcode);
                departmentVo1.setDepname(depname);
                departmentVoList.add(departmentVo1);
            }
            departmentVo.setChildren(departmentVoList);
            depList.add(departmentVo);
        }
        return depList;
    }
}
