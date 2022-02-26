package com.atguigu.yygh.common.handler;

import com.atguigu.yygh.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;


import org.springframework.web.bind.annotation.RestControllerAdvice;
/*
*
*AOP AfterThrowing
* */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ZDYException.class)
    public R error(ZDYException e){
        e.printStackTrace();
        log.error(e.getMessage());
        return R.error().code(e.getCode()).message(e.getMsg());
    }
    @ExceptionHandler(Exception.class)
    public R error(Exception e){
        e.printStackTrace();
        return R.error();
    }

}
