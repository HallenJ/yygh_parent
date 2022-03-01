package com.atguigu.yygh.dict.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.yygh.common.handler.ZDYException;
import com.atguigu.yygh.dict.listener.DictListener;
import com.atguigu.yygh.dict.mapper.DictMapper;
import com.atguigu.yygh.dict.service.DictService;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {
    @Autowired
    private DictListener dictListener;
    @Override
    @Cacheable(value = "dict",key ="'selectIndexList'+#id")
    public List<Dict> findChlidData(Long id) {
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", id);
        List<Dict> dicts = baseMapper.selectList(queryWrapper);
        for (Dict dict : dicts) {
            Long id1 = dict.getId();
            dict.setHasChildren(hasChildren(id1));
        }
        return dicts;
    }

    /*
     *导出文件
     * */
    @Override
    public void exportData(HttpServletResponse response) {
        try {
            //1.设置响应数据
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode("数据字典", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            //2.进行数据查询
            QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
            List<Dict> dicts = baseMapper.selectList(queryWrapper);
            List<DictEeVo> dictEeVoList = new ArrayList<>();
            //3.进行数据转换
            for (Dict dict : dicts) {
                DictEeVo dictEeVo = new DictEeVo();
                BeanUtils.copyProperties(dict, dictEeVo);
                dictEeVoList.add(dictEeVo);
            }
            //4.返回数据封装Excel
            EasyExcel.write(response.getOutputStream()).sheet("表1").doWrite(dictEeVoList);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ZDYException(20000, "出现错误了┭┮﹏┭┮");
        }
    }
    /*
    *进行文件写入
    * */
    @Override
    public void importData(MultipartFile multipartFile){

        try {
            System.out.println("multipartFile = " + multipartFile);
            EasyExcel.read(multipartFile.getInputStream(),DictEeVo.class,dictListener).sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ZDYException(5001,"出错了");
        }
    }

    @Override
    public String getNameByParentDictCodeAndValue(String parentDictCode, String value) {
        //如果value能唯一定位数据字典，parentDictCode可以传空，例如：省市区的value值能够唯一确定
        if(StringUtils.isEmpty(parentDictCode)) {
            Dict dict = baseMapper.selectOne(new QueryWrapper<Dict>().eq("value", value));
            if(null != dict) {
                return dict.getName();
            }
        } else {
            Dict parentDict = this.getDictByDictCode(parentDictCode);
            if(null == parentDict) return "";
            Dict dict = baseMapper.selectOne(new QueryWrapper<Dict>().eq("parent_id",
                    parentDict.getId()).eq("value", value));
            if(null != dict) {
                return dict.getName();
            }
        }
        return "";
    }

    @Override
    public List<Dict> findByDictCode(String dictCode) {
        Dict dictByDictCode = this.getDictByDictCode(dictCode);
        List<Dict> list = baseMapper.selectList(
                new QueryWrapper<Dict>()
                        .eq("parent_id", dictByDictCode.getId()));
        return list;
    }


    //实现方法 根据dict_code查询
    private Dict getDictByDictCode(String dictCode) {
        QueryWrapper<Dict> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_code",dictCode);
        Dict codeDict = baseMapper.selectOne(wrapper);
        return codeDict;
    }

    private Boolean hasChildren(Long id) {
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", id);
        Integer integer = baseMapper.selectCount(queryWrapper);
        return integer > 0;
    }
}
