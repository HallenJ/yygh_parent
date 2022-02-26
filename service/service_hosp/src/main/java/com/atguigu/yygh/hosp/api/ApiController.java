package com.atguigu.yygh.hosp.api;


import com.atguigu.yygh.common.Result;
import com.atguigu.yygh.common.handler.ZDYException;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.service.impl.DepartmentServiceImpl;
import com.atguigu.yygh.hosp.utils.HttpRequestHelper;
import com.atguigu.yygh.hosp.utils.MD5;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.sql.ResultSet;
import java.util.Map;

@Api(tags = "接口")
@RestController
@RequestMapping("/api/hosp")
public class ApiController {
    @Autowired
    private HospitalService hospitalService;
    @Autowired
    private HospitalSetService hospitalSetService;
    @Autowired
    private DepartmentServiceImpl departmentService;

    @ApiOperation("接收医院信息")
    @PostMapping("saveHospital")
    public Result saveHospital(HttpServletRequest request) {
        //获取并转化参数????????
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> stringObjectMap = HttpRequestHelper.switchMap(parameterMap);
        //进行MD5检验
        String hoscode = (String) stringObjectMap.get("hoscode");
        String sign = (String) stringObjectMap.get("sign");

        String hospSign = hospitalSetService.getSignKey(hoscode);
        String encrypt = MD5.encrypt(hospSign);

        if (sign.equals(encrypt)) {
            //data为了能正常显示需要进行转化
            String logoData = (String) stringObjectMap.get("logoData");
            logoData = logoData.replaceAll(" ", "+");
            stringObjectMap.put("logoData", logoData);
            //掉service层
            hospitalService.saveHospital(stringObjectMap);
            //返回Result
            return Result.ok();
        } else {
            throw new ZDYException(200, "秘钥校验失败");
        }
    }

    @ApiOperation(value = "获取医院信息")
    @PostMapping("hospital/show")
    public Result hospital(HttpServletRequest request) {
        //1获取并转化参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(parameterMap);
        //2签名校验 省略
        String hoscode = (String) paramMap.get("hoscode");
        String sign = (String) paramMap.get("sign");
        //3查询返回
        Hospital hospital = hospitalService.getHospByHoscode(hoscode);
        return Result.ok(hospital);
    }


    @ApiOperation("上传科室")
    @PostMapping("saveDepartment")
    public Result saveDepartment(HttpServletRequest request) {
        //1获取并转化参数
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> paramMap = HttpRequestHelper.switchMap(parameterMap);
        //2.进行校验
        //调用下一层
        departmentService.saveDepartment(parameterMap);
        //返回Result
        return Result.ok();
    }

    @ApiOperation("带条件和分页查询部门")
    @PostMapping("department/list")
    public Result departmentList(HttpServletRequest request) {
        //将传输数据进行转换
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> stringObjectMap = HttpRequestHelper.switchMap(parameterMap);
        //进行校验省略
        //进行封装校验
        String stringLimit = (String) stringObjectMap.get("limit");
        String stringPage = (String) stringObjectMap.get("page");
        int limit = StringUtils.isEmpty(stringLimit) ? 10 : Integer.parseInt(stringLimit);
        int page = StringUtils.isEmpty(stringPage) ? 1 : Integer.parseInt(stringPage);
        HospitalQueryVo hospitalQueryVo = new HospitalQueryVo();
        hospitalQueryVo.setHoscode((String) stringObjectMap.get("hoscode"));

          Page<Department> page1 =departmentService.departmentList(limit, page, hospitalQueryVo);

        return Result.ok(page1);

    }
    @ApiOperation("部门删除")
    @PostMapping("department/remove")
    public Result deleteDept(HttpServletRequest request){
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, Object> stringObjectMap = HttpRequestHelper.switchMap(parameterMap);
        //进行校验
        //传递两个参数进入service
        departmentService.deleteDept(
                (String) stringObjectMap.get("hoscode"),
                (String) stringObjectMap.get("depcode")
        );
        return Result.ok();
    }
}
