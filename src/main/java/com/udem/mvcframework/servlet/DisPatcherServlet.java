package com.udem.mvcframework.servlet;

import com.udem.mvcframework.annotation.Autowired;
import com.udem.mvcframework.annotation.Controller;
import com.udem.mvcframework.annotation.RequestMapping;
import com.udem.mvcframework.annotation.Service;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 前端请求中央处理器
 */
public class DisPatcherServlet extends HttpServlet {

    /**
     * 封装配置文件属性
     */
    private final static Properties properties = new Properties();

    /**
     * 存放扫描类的全限定命名
     */
    private final static Set<String> stringSet = new HashSet<>();

    /**
     * .class
     */
    public static final String CLASS_STR = ".class";


    /**
     * 存储bean单例
     */
    public final static Map<String, Object> IOC_MAP = new HashMap<>();


    /**
     * 构建映射信息
     */
    public final static List<Handle> HANDLE_MAPPING = new ArrayList<>();


    @Override
    public void init() throws ServletException {
        //加载流
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("mvc.properties");

        System.out.println("----------------- 初始化 -------------------");
        super.init();


        try {
            //1 加载配置文件
            doLoadConfigFile(resourceAsStream);
            // 2 扫描包
            doScanPackage();
            // 3  bean实例化和依赖维护
            doInitBean();
            // 4 维护url和method之间的映射关系
            doHandlerMapping();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("------------  ioc容器bean \n " + IOC_MAP);

        System.out.println("----------------- 初始化完成 -------------------");
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //5 判断请求Url和方法url 执行相应方法
        String requestURI = req.getRequestURI();
        //遍历请求中得参数顺序
        Map<String, String[]> reqParameterMaps = req.getParameterMap();

        System.out.println("------- 当前请求的url是 : " +  requestURI);
        if (Objects.isNull(requestURI) || requestURI.length() == 0) {
            return;
        }
        //5.1 获取handle
        Handle handler = getHandler(req);

        if (Objects.isNull(handler)){
            resp.getWriter().println("404....");
            return;
        }
        //获取执行方法
        Method method = handler.getMethod();

        Parameter[] parameters = handler.getMethod().getParameters();
        //5.2 获取型参数
        Map<String, Integer> map1 = handler.getMap();
        //5.3 获取参数长度
        //设置传递参数
        Object[] objects = new Object[parameters.length];

        //保证参数顺序和方法中的型参数顺序一致

        //遍历除reqest 和 response之外的参数 并且填充到参数数组里
        for (Map.Entry<String, String[]> stringEntry : reqParameterMaps.entrySet()) {
            //判断请求参数和形参是否参数匹配
            //多个参数直接 , 拼接
            String value = StringUtils.join(stringEntry.getValue(), ",");  // 如同 1,2
            if (!map1.containsKey(stringEntry.getKey())) {
               continue;
            }
            //获取型参数索引
            Integer index = map1.get(stringEntry.getKey());

            //赋值参数
            objects[index] = value;
        }

        int index1 = map1.get(HttpServletRequest.class.getSimpleName());
        int index2 = map1.get(HttpServletResponse.class.getSimpleName());
        //获取ret对象参数
        objects[index1] = req;
        //获取resp对象参数
        objects[index2] = resp;

        try {
           String result = (String) method.invoke(handler.getController(), objects);
           resp.getWriter().println(result);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    private Handle getHandler(HttpServletRequest req) {

        // 根据url找到对应得method方法 并且执行调用
        for (Handle handle : HANDLE_MAPPING) {


            Matcher matcher = handle.getPattern().matcher(req.getRequestURI());
            if (matcher.find()) {
                return handle;
            }


        }

        return null;
    }

    /**
     * 加载配置文件
     *
     * @param resourceAsStream
     */
    private void doLoadConfigFile(InputStream resourceAsStream) {
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * handlerMapper 处理映射器
     * 建立url和方法一致
     */
    private void doHandlerMapping() {

        if (IOC_MAP.isEmpty()) {
            return;
        }

        //只处理controller层
        for (Map.Entry<String, Object> stringObjectEntry : IOC_MAP.entrySet()) {

            Class<?> objectEntryValue = stringObjectEntry.getValue().getClass();
            //没有包含 注解 跳过
            if (!objectEntryValue.isAnnotationPresent(Controller.class)) {
                continue;
            }

            String url = "";
            //包含 RequestMapping 注解 就获取url
            if (objectEntryValue.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping requestMapping = objectEntryValue.getAnnotation(RequestMapping.class);
                url = requestMapping.value();
            }


            //获取方法
            Method[] methods = objectEntryValue.getMethods();
            if (methods.length <= 0 || Objects.isNull(methods)) {
                continue;
            }

            for (Method method : methods) {
                //如果不包含就跳过
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }
                //获取当前方法上 RequestMapping 的注解
                RequestMapping requestMapping = method.getDeclaredAnnotation(RequestMapping.class);
                //构建url
                String urls = url + requestMapping.value();

                //封装handler
                Handle handle = new Handle(stringObjectEntry.getValue(), Pattern.compile(urls), method);

                //处理计算参数位置
                Parameter[] parameters = method.getParameters();
                if (parameters != null && parameters.length > 0) {
                    for (int i = 0; i < parameters.length; i++) {
                        Parameter parameter = parameters[i];
                        if (parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                            //如果是 参数名称存本身
                            handle.getMap().put(parameter.getType().getSimpleName(), i);
                        }
                        //如果是tring类型
                        else {
                            //String类型存储name
                            handle.getMap().put(parameter.getName(), i);
                        }
                        //其他类型暂时不做判断
                    }
                    HANDLE_MAPPING.add(handle);
                }


            }

        }


    }


    /**
     * bean实例化和bean依赖注入关系维护
     *
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    private void doInitBean() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        for (String className : stringSet) {
            //反射创建对象
            Class<?> aClass = Class.forName(className);
            //查找 Controller 注解
            Controller annotation = aClass.getAnnotation(Controller.class);
            if (Objects.nonNull(annotation)) {
                //实例化存入ioc
                IOC_MAP.put(aClass.getSimpleName(), aClass.newInstance());
            }
            //查找 service 注解
            Service service = aClass.getAnnotation(Service.class);
            if (Objects.nonNull(service)) {
                //实例化存入ioc
                IOC_MAP.put(aClass.getSimpleName(), aClass.newInstance());
                //如果实现类接口 按照接口实例化
                Class<?>[] interfaces = aClass.getInterfaces();
                if (Objects.nonNull(interfaces) && interfaces.length > 0) {
                    for (Class<?> anInterface : interfaces) {
                        //按照接口名进行实例化
                        IOC_MAP.put(anInterface.getSimpleName(), aClass.newInstance());
                    }

                }
            }
        }


        //依赖注入
        for (Map.Entry<String, Object> stringObjectEntry : IOC_MAP.entrySet()) {

            Class aClass = stringObjectEntry.getValue().getClass();
            //获取所有字段
            Field[] declaredFields = aClass.getDeclaredFields();

            if (Objects.isNull(declaredFields) && declaredFields.length == 0) {
                continue;
            }

            for (Field field : declaredFields) {
                //field.get
                //字段含有 Autowired 注解的需要被自动装配对象
                Autowired autowired = field.getAnnotation(Autowired.class);

                if (Objects.nonNull(autowired)) {
                    //根据当前key获取需要注入示例对象
                    //先根据名字注入,如果名字获取不到,再根据类型去注入
                    String beanName = autowired.name();

                    if (Objects.isNull(beanName) || beanName.length() == 0) {
                        beanName = field.getType().getSimpleName();
                    }

                    //反射设置值
                    try {
                        field.setAccessible(true);
                        //自动装配 线程不安全,Spring中默认单例
                        field.set(stringObjectEntry.getValue(), IOC_MAP.get(beanName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }


    /**
     * 包扫描获取字节码对象
     *
     * @throws UnsupportedEncodingException
     */
    private void doScanPackage() throws UnsupportedEncodingException {
        //获取mvc包路径
        String packName = properties.getProperty("mvc.path");
        if (Objects.isNull(packName) || packName.length() == 0) {
            throw new RuntimeException("无效的包路径");
        }
        packName = packName.replace(".", File.separator);
        URL resource = this.getClass().getClassLoader().getResource(packName);
        String path = resource.getPath();
        //解析中文
        String filePath = URLDecoder.decode(path, "UTF-8");

        //解析包成java权限定命名com
        parseFilePackName(packName, stringSet, filePath);

    }


    /**
     * 递归处理路径下文件夹是否包含文件夹,如不包含则获取当前类的权限定命名存入set中
     *
     * @param packName
     * @param classNameSet
     * @param path
     */
    public static void parseFilePackName(String packName, Set<String> classNameSet, String path) {

        File packNamePath = new File(path);

        if (!packNamePath.isDirectory() || !packNamePath.exists()) {
            return;
        }
        //递归路径下所有文件和文件夹
        for (File file : packNamePath.listFiles()) {
            boolean directory = file.isDirectory();
            String classNamePath = packName + File.separator + file.getName().replace(File.separator, ".");
            if (directory) {
                parseFilePackName(classNamePath, classNameSet, file.getPath());
            } else if (file.isFile() && file.getName().endsWith(CLASS_STR)) {
                //存入set
                classNameSet.add(classNamePath.replace(File.separator, ".").replace(CLASS_STR, ""));
            }
        }

    }
}
