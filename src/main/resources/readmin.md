
## 自定义mvc框架


## 步骤

 - 1 启动tomcat 
     DisPatcherServlet 加载配置文件SpringMvc.xml
 
 - 2 指定包扫描,扫描注解
    
    @Controllelr
    @Service
    @Autowired
    @RequestMapping 维护url映射
      
 - 3 加入IOC容器
     初始化对象,维护依赖关系,维护url映射
      
 - 4 SpringMVC相关组件初始化
  
     简历Url和method之间的映射关系---> 映射器 HanderMappping
     
     
 - 5 请求处理