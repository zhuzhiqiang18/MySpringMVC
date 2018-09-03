package com.zzq.demo.controller;


import com.zzq.framework.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/test")
public class TestController {
    @Autowired
    Service service;
    @RequestMapping("/test")
    public void test(HttpServletResponse response, HttpServletRequest request, @RequestParam("name") String name)throws Exception{
        response.getWriter().write(name);
        response.getWriter().flush();
        response.getWriter().close();
    }

}
