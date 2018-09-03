package com.zzq.framework;

import com.zzq.framework.annotation.Controller;
import com.zzq.framework.annotation.RequestMapping;
import com.zzq.framework.annotation.RequestParam;
import com.zzq.framework.context.ApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class DispatcherServlet extends HttpServlet {
    private  final String CONFLOCATION="confLocation";
    private  ApplicationContext applicationContext;

    private List<DispatcherServlet.Handler> handlerMapping = new ArrayList<>();

    private Map<Handler,HandlerAdapter> adapterMapping = new HashMap<Handler, HandlerAdapter>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try{
            doDispatch(req,resp);
        }catch(Exception e){
            resp.getWriter().write("500 Exception, Msg :" + Arrays.toString(e.getStackTrace()));
        }
    }

    //处理请求
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
            try{
                Handler handler= getHandler(req);
                if(handler==null){
                    resp.getWriter().write("404 Not Found");
                    return ;
                }
              HandlerAdapter handlerAdapter=  getHandlerAdapter(handler);

               handlerAdapter.handle(req,resp,handler);
            }catch (Exception e){
                e.printStackTrace();
            }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("==================容器初始化STAR=======================");
        String confFile= config.getInitParameter(CONFLOCATION);
        applicationContext = new ApplicationContext(confFile);
        System.out.println("==================容器初始化END=======================");
        System.out.println("IOC容器"+applicationContext.getAll());

        //解析url和Method的关联关系
        initHandlerMappings(applicationContext);
        //适配器（匹配的过程）
        initHandlerAdapters(applicationContext);

    }

    /**
     * 初始化Handler
     * @param applicationContext
     */
    private void initHandlerMappings(ApplicationContext applicationContext) {
        for(Map.Entry<String,Object> entry: applicationContext.getAll().entrySet()){
            //先判断是否是Controller
            Class<?> clazz=entry.getValue().getClass();

            if(!clazz.isAnnotationPresent(Controller.class)){continue;}
            String url="";
            //获取Controller requestMapping
            if(clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping= clazz.getAnnotation(RequestMapping.class);
                url+= requestMapping.value();
            }
            //方法
            Method [] methods= clazz.getMethods();
            for(Method method:methods){
                if(!method.isAnnotationPresent(RequestMapping.class)){continue;}

                RequestMapping requestMapping= method.getAnnotation(RequestMapping.class);
                url+=requestMapping.value();
                DispatcherServlet.Handler handle = new Handler(url,entry.getValue(),method);
                handlerMapping.add(handle);
                System.out.println(handle.toString());
            }
        }
    }


    private void initHandlerAdapters(ApplicationContext applicationContext) {
        if(handlerMapping.isEmpty()){return;}

        //参数类型作为key，参数的索引号作为值
        Map<String,Integer> paramMapping = new HashMap<String,Integer>();


        for (Handler handler :handlerMapping){
            Class<?>[] parameterTypes= handler.method.getParameterTypes();
            for (int i=0;i<parameterTypes.length;i++){
                Class<?> paraType= parameterTypes[i];
                if(paraType==HttpServletRequest.class||paraType==HttpServletResponse.class){
                    paramMapping.put(paraType.getName(),i);
                }
            }

            //这里是匹配Request和Response
            Annotation[][] pa = handler.method.getParameterAnnotations();
            for (int i = 0; i < pa.length; i ++) {
                for(Annotation a : pa[i]){
                    if(a instanceof RequestParam){
                        String paramName = ((RequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramMapping.put(paramName, i);
                        }

                    }
                }
                adapterMapping.put(handler,new HandlerAdapter(paramMapping));
            }

        }


    }

    private class Handler{
        private String url;
        private Object controller;
        private Method method;

        public Handler(String url, Object controller, Method method) {
            this.url = url;
            this.controller = controller;
            this.method = method;
        }

        public String getUrl() {
            return url;
        }

        public Object getController() {
            return controller;
        }

        public Method getMethod() {
            return method;
        }

        @Override
        public String toString() {
            return "Handler{" +
                    "url='" + url + '\'' +
                    ", controller=" + controller +
                    ", method=" + method +
                    '}';
        }
    }



    private class HandlerAdapter{
        private Map<String,Integer> paramMapping;
        public HandlerAdapter(Map<String,Integer> paramMapping){
            this.paramMapping = paramMapping;
        }

        public void handle(HttpServletRequest req, HttpServletResponse resp,Handler handler){
          Class<?> paraType[]=  handler.method.getParameterTypes();

          Map<String,String[]> params = req.getParameterMap();

          Object[] paramValues = new Object[paraType.length];

          for (Map.Entry<String,String[]> param:params.entrySet()){
              String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

              if(!this.paramMapping.containsKey(param.getKey())){continue;}

              int index = this.paramMapping.get(param.getKey());
              //单个赋值是不行的
              paramValues[index] = castStringValue(value,paraType[index]);
          }

            //request 和 response 要赋值
            String reqName = HttpServletRequest.class.getName();
            if(this.paramMapping.containsKey(reqName)){
                int reqIndex = this.paramMapping.get(reqName);
                paramValues[reqIndex] = req;
            }


            String resqName = HttpServletResponse.class.getName();
            if(this.paramMapping.containsKey(resqName)){
                int respIndex = this.paramMapping.get(resqName);
                paramValues[respIndex] = resp;
            }

            try {
                handler.method.invoke(handler.controller, paramValues);
                //此处关于Controller 方法带有返回视图  待优化
                //todo
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }


        }
        private Object castStringValue(String value,Class<?> clazz){
            if(clazz == String.class){
                return value;
            }else if(clazz == Integer.class){
                return Integer.valueOf(value);
            }else if(clazz == int.class){
                return Integer.valueOf(value).intValue();
            }else{
                return null;
            }
        }
    }

    private Handler getHandler(HttpServletRequest req){
        //循环handlerMapping
        if(handlerMapping.isEmpty()){ return null; }

        //
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        String urls[]=url.split("\\.");

        for (Handler handler : handlerMapping) {
            if(handler.url.equals(urls[0])){
                return handler;
            }
        }

        return null;

    }

    private HandlerAdapter getHandlerAdapter(Handler handler){
        if(adapterMapping.isEmpty()){return null;}
        return adapterMapping.get(handler);
    }



}
