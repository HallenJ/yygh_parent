package com.atguigu.yygh.dict.controller;

import com.atguigu.yygh.common.R;
import com.atguigu.yygh.dict.service.DictService;
import com.atguigu.yygh.model.cmn.Dict;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Api(description = "数据字典接口")
@RestController
@RequestMapping("/admin/cmn/dict")
@CrossOrigin
public class DictController {
    @Autowired
    private DictService dictService;

    //根据数据id查询子数据列表
    @ApiOperation(value = "根据数据id查询子数据列表")
    @GetMapping("findChildData/{id}")
    public R findChildData(@PathVariable Long id) {
        List<Dict> list = dictService.findChlidData(id);
        return R.ok().data("list",list);
    }

    @ApiOperation("将文件进行导出")
    @GetMapping("exportData")
    public void exportData(HttpServletResponse response){
        dictService.exportData(response);
    }


    @ApiOperation("将文件进行导入数据库")
    @PostMapping("importData")
    public R importData(MultipartFile file) throws IOException {
        System.out.println(file);
        dictService.importData(file);
        return R.ok();
    }
}
