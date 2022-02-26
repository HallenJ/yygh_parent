package com.atguigu.yygh.hosp.service;

import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface DepartmentService {
    //存储部门信息
    void saveDepartment(Map<String, String[]> parameterMap);
    //带条件查询分页部门
    Page<Department> departmentList(int limit, int page, HospitalQueryVo hospitalQueryVo);
    //部门删除
    void deleteDept(String hoscode, String depcode);
}
