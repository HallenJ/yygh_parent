package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.common.handler.ZDYException;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalSetQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(description = "医院设置接口")
@RestController
@RequestMapping("/admin/hosp/hospitalSet")
@CrossOrigin
public class HospitalSetController {
    @Autowired
    private HospitalSetService hospitalSetService;

    //code: 20000, data: {token: "admin-token"}
    @ApiOperation(value = "登录系统login")
    @PostMapping("login")
    public R login() {
        return R.ok().data("token", "admin-token");
    }

    // "data":{"roles":["admin"],
// "introduction":"I am a super administrator",
// "avatar":"https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif",
// "name":"Super Admin"}
    @ApiOperation("登录系统的pageInfo")
    @GetMapping("getInfo")
    public R getInfo() {
        Map<String, Object> map = new HashMap<>();
        map.put("roles", "[admin]");
        map.put("introduction", "I am a super administrator");
        map.put("avatar", "https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif");
        map.put("name", "Super Admin");
        return R.ok().data(map);
    }


    @ApiOperation(value = "医院设置列表")
    @GetMapping("findAll")
    public R findAll() {
        /*try {
            int i = 10 / 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZDYException(10086, "这是一个沙雕错误");
        }*/
        List<HospitalSet> list = hospitalSetService.list();
        return R.ok().data("list", list);
    }

    @ApiOperation(value = "医院设置删除")
    @DeleteMapping("{id}")
    public R removeById(@ApiParam(name = "id", value = "讲师ID", required = true) @PathVariable String id) {
        hospitalSetService.removeById(id);
        return R.ok();
    }

    @ApiOperation("进行分页和带条件查询")
    @PostMapping("pageQuery/{page}/{limit}")
    public R pageQuery(@PathVariable Long page,
                       @PathVariable Long limit,
                       @RequestBody HospitalSetQueryVo hospitalSetQueryVo) {
        Page<HospitalSet> pageParam = new Page<>(page, limit);
        QueryWrapper<HospitalSet> queryWrapper = new QueryWrapper<>();
        if (!StringUtils.isEmpty(hospitalSetQueryVo.getHosname())) {
            queryWrapper.like("hosname", hospitalSetQueryVo.getHosname());
        }
        if (!StringUtils.isEmpty(hospitalSetQueryVo.getHoscode())) {
            queryWrapper.like("hoscode", hospitalSetQueryVo.getHoscode());
        }
        hospitalSetService.page(pageParam, queryWrapper);
        return R.ok().data("total", pageParam.getTotal()).data("list", pageParam.getRecords());
    }

    @ApiOperation("根据id进行查询并回显")
    @GetMapping("findHospSetById/{id}")
    public R findHospSetById(@PathVariable Long id) {
        return R.ok().data("HospSet", hospitalSetService.getById(id));
    }

    @ApiOperation("更新传回来的数据")
    @PutMapping("updateHospSet")
    public R updateHospSet(@RequestBody HospitalSet hospitalSet) {
        boolean updateById = hospitalSetService.updateById(hospitalSet);
        if (updateById) {
            return R.ok();
        } else {
            return R.error();
        }
    }

    @ApiOperation("批量删除")
    @DeleteMapping("delete")
    public R deleteHospSetByIds(@RequestBody List<Integer> list) {
        boolean remove = hospitalSetService.removeByIds(list);
        if (remove) {
            return R.ok();
        } else {
            return R.error();
        }
    }

    @ApiOperation("上锁和解锁")
    @PutMapping("lockHospitalSet/{id}/{status}")
    public R lockHospitalSet(@PathVariable Long id,
                             @PathVariable Integer status
    ) {
        HospitalSet byId = hospitalSetService.getById(id);
        byId.setStatus(status);
        boolean lockHospitalSet = hospitalSetService.updateById(byId);
        if (lockHospitalSet) {
            return R.ok();
        } else {
            return R.error();
        }
    }

    @ApiOperation("新增数据")
    @PostMapping("insertInto")
    public R insertInto(@RequestBody HospitalSet hospitalSet) {
        boolean save = hospitalSetService.save(hospitalSet);
        if (save) {
            return R.ok();
        } else {
            return R.error();
        }
    }
}
