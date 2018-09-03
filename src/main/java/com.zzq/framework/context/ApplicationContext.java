package com.zzq.framework.context;

import com.zzq.framework.annotation.Autowired;
import com.zzq.framework.annotation.Controller;
import com.zzq.framework.annotation.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {

    private Map<String,Object> instanceMapping = new ConcurrentHashMap<String, Object>();

    //类似于内部的配置信息，我们在外面是看不到的
    //我们能够看到的只有ioc容器  getBean方法来间接调用的
    private List<String> classCache = new ArrayList<String>();


    public ApplicationContext(String location) {
        InputStream inputStream= getClass().getClassLoader().getResourceAsStream(location);
        Properties prop = new Properties();
        try {
            prop.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //注册
        String scanBase= prop.getProperty("scanBase");
        doReg(scanBase);

        //实例化IOC
        createBaen(classCache);
        //注入
        populate();

    }

    //注入
    private void populate() {
        if(instanceMapping.isEmpty()){return;}

        for(Map.Entry<String,Object> entry :instanceMapping.entrySet()){
            Field[] fields= entry.getValue().getClass().getFields();
            for(Field field :fields){

                //将带有Autowired  注入IOC中的实例
                if(field.isAnnotationPresent(Autowired.class)){
                    String id=field.getType().getName();

                    //可以操作私有变量
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(),instanceMapping.get(id));
                    }catch (Exception e){
                        e.printStackTrace();
                        continue;
                    }
                }
            }
        }
    }

    //初始化Controller Service
    private void createBaen(List<String> classCache) {
        if(classCache.size()==0){return;}

        try{
            for (String bean :classCache){
                Class<?> clazz=Class.forName(bean);
                //判断是否是controller
                if(clazz.isAnnotationPresent(Controller.class)){
                    //类名首字母小写
                    String id=lowerFirstChar(clazz.getSimpleName());
                    //实例化类放入IOC 容器
                    instanceMapping.put(id,clazz.newInstance());

                    //Service  此处Service不处理别名 默认首字母小写  约定大于配置
                }else if(clazz.isAnnotationPresent(Service.class)){
                    String id=lowerFirstChar(clazz.getSimpleName());
                    instanceMapping.put(id,clazz.newInstance());

                    //Service大多有接口 接口绑定实现类

                    Class<?>[] interfaces= clazz.getInterfaces();
                    for (Class<?> c :interfaces){
                        instanceMapping.put(c.getName(),clazz.newInstance());
                    }

                }else{continue;}
            }
        }catch (Exception e){

        }

    }


    //注册bean到IOC容器
    public void doReg(String scanBase){
        URL url= getClass().getClassLoader().getResource("/"+scanBase.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for (File file:dir.listFiles()){
            if(file.isDirectory()){
                doReg(scanBase+"."+file.getName());
            }else {
                classCache.add(scanBase+"."+file.getName().replaceAll(".class",""));
            }
        }

    }


    /**
     * 将首字母小写
     * @param str
     * @return
     */
    private String lowerFirstChar(String str){
        char [] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    public Map<String, Object> getAll() {
        return instanceMapping;
    }
}
