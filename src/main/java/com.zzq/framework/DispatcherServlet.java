package com.zzq.framework;

import com.zzq.framework.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {
    private  final String CONFLOCATION="confLocation";
    private  ApplicationContext applicationContext;
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("==================容器初始化STAR=======================");
        String confFile= config.getInitParameter(CONFLOCATION);
        applicationContext = new ApplicationContext(confFile);
        System.out.println("==================容器初始化END=======================");

        System.out.println("IOC容器"+applicationContext.getAll());
    }
}
