# 基于Reactor架构的http服务器及其开发框架

## 概述

这是一个用原生 Java NIO 包实现的 http服务器，在此基础上，我们还提供了如下功能，使它更像一个开发框架！

- Interceptor
- Controller
- 视图解析
- Ioc 容器

该项目没有使用 jdk 之外的第三方包。

喜欢这个项目吗？给作者一颗星表达你的支持！

## 快速上手

本代码包含了一个实例，通过这个示例你可以了解到具体的内容。

1. 使用配置文件进行Ioc注入。
2. 构建拦截器。
3. 用Controller实现业务逻辑处理。

项目结构概述：

```txt
+ src
|___+ com
    |___ ... ... 这里是框架和服务器代码
|___+ test
    |___+ controller            <--- 控制器
        |___MyController.java
    |___+ interceptor           <--- 拦截器
        |___MyInterceptor1.java
        |___MyInterceptor2.java
    |___+ unit
        |___ServerInit.java     <--- 应用程序启动类
    |___+ iocxml                <--- Ioc 配置
        |___ bean.xml
    |___config.property         <--- 整体配置
|___+ static                    <--- 静态资源
    |___+ html
        |___index.html
    |___+ css
        |___mycss.css
    |___+ javascript
        |___myjs.js

```

### 配置文件

以下是 config.property 的内容：

```txt
port: 18080
controller-base-package: test/controller
interceptor-base-pacakge: test/interceptor
interceptors: MyInterceptor1, MyInterceptor2
static: src/static
ioc-xml-path: src/test/iocxml/bean.xml
use-main-sub-reactor: true
sub-reactor-count: 1
```

参数说明：

**port**: 服务器运行的端口

**controller-base-package**: Controller 所在的包，包中放置控制器，所谓控制器，就是负责业务逻辑处理的部分。

**interceptor-base-package**: Interceptor 所在的包，包中放置拦截器，所谓拦截器，就是拦截请求，并对其进行校验，决定是否让它进入业务逻辑处理的 java 类。

**interceptors**: 用逗号分割，指定了全部用于请求过滤的拦截器，分先后顺序，前面的拦截器将最先执行。

**static**: 静态资源存放根目录。

**ioc-xml-path**: Ioc容器功能的配置文件。

**use-main-sub-reactor**: 是否使用主从reactor模型，如果 false，使用的是单 reactor 模型。

**sub-reactor-count**: 从 reactor（netty 的 worker）数量。

### 启动应用程序

 `ServerInit.java` 中包含了 `main` 方法。

```java
package tutorial;

import com.galaxyzeta.server.reactor.WebApplicationContext;

public class MyApplication {
	public static void main(String[] args) {
		new WebApplicationContext().runApplication("src/tutorial/config.property");
	}
}

```

### 控制器

`MyController.java` 包含了业务逻辑控制器的写法。

```java
package test.controller;

import com.galaxyzeta.annotation.RequestMapping;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.Controller;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;
import com.galaxyzeta.util.ResponseFactory;

public class MyController implements Controller {

	private static Logger LOG = LoggerFactory.getLogger(MyController.class);

	@RequestMapping(method = "GET", url = "/debug")
	public Object debugGet(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("GET /debug invoked OK");
		return "html/index.html";
	}

	@RequestMapping(method = "GET", url = "/json")
	public Object debugPost(HttpRequest req, HttpResponse resp) {
		LOG.DEBUG("POST /debug invoked OK");
		HttpResponse myResponse = ResponseFactory.getSuccess();
		resp.setResponseBody("Hello this is a json view object!");
		// resp.setResponseBody("hello this is a response body");
		return myResponse;
	}
}
```

目前控制器支持的内容：

1. GET/POST/PUT/DELETE 方法的解析。
2. 执行指定 url 相应的业务逻辑。
3. 返回 **视图字符串** 或者 **HttpResponse**

简单的解释一下这个控制器：这个控制器包含两个方法：

- 一个处理 GET /debug 请求的方法，方法被invoke时，返回 `html/index.html` 这个**视图对象**。
- 一个处理 POST /debug 请求的方法，方法在运行时返回 myResponse 对象视图。

**一些重要说明**：

- 控制器必须放在配置文件指定的包下，否则不会被检测到。
- 作为控制器的方法必须附带 `@RequestMapping(method = "xxx", url = "xxx")`。
- 项目对业务逻辑方法的限制：参数必须有 `HttpReqeust` 和 `HttpResponse`。
- 如果 controller 方法返回 null，参数 resp 将被作为 Response 写回给浏览器。
- 如果 controller 方法返回视图物体，参数 resp 不会起到作用。

### 拦截器

拦截器用于对请求进行过滤，同样的我们编写两个简单的拦截器 `MyInterceptor1.java`, `MyInterceptor2.java`

```java
package test.interceptor;

import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.Interceptor;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public class MyInterceptor1 implements Interceptor {

    private static Logger LOG = LoggerFactory.getLogger(MyInterceptor1.class);

    public static boolean intercept(HttpRequest req, HttpResponse resp) {
        LOG.DEBUG("请求正在经过 [拦截器1] 的过滤");
        return true;
    }
}
```

这两个拦截器什么都没干，只是输出日志，并返回 `true`。

返回 `true` 的含义是放行，请求将前往下一个拦截器接受检查。当然你可以对 request 进行检查，禁止某些 request 通过拦截器，只需返回  `false` 即可。

根据配置文件决定拦截器执行顺序。MyInterceptor1 首先作用，而 MyInterceptor2 将在请求通过前置拦截器的检查后发挥作用。

**一些重要说明：**

- 拦截器必须放在指定的包下。
- 拦截器的方法规范：必须是 `public static boolean intercept(HttpRequest req, HttpResponse resp)` 不能有任何变动。
- 若你的某个拦截器返回 `false` ，控制器方法不会被执行，参数 resp 将作为最终 response 被写回浏览器。

### 运行Demo

回到 `TestAPI.java` 运行 Demo，得到以下结果：

```txt
[INFO]  WebApplicationContext -- 配置文件解析完毕
[INFO]  WebApplicationContext -- 业务逻辑处理器 解析成功
[INFO]  WebApplicationContext -- 拦截器 test.interceptor.MyInterceptor1 注册成功
[INFO]  WebApplicationContext -- 拦截器 test.interceptor.MyInterceptor2 注册成功
[INFO]  ReactorServer -- 正在启动... ...
[INFO]  ReactorServer -- 已在端口 8080 上启动服务.
```

此时服务已经启动，打开浏览器，输入 `localhost:8080/debug`，可以立即得到一个 html 页面:

（以下是浏览器显示的内容）

----



<h1>Hello html</h1>
<button onclick="popAlert('Test JS OK')">Test JS</button>

<div id="custom">
    This should be colored cyan.
</div>

----

按下按钮可以弹出一个 `alert` ，下面的文字是青色，则说明项目已经运行成功。

## 项目架构

下面简单的描述以下 Reactor Server 和 开发框架的工作过程。

### ReactorServer 运作流程

我以主从 Reactor 为例，讲解项目启动的过程：

1. `WebApplication.runApplication` 启动应用程序上下文。
2. `WebApplication` 启动过程中，读取 `config.property` 和 `bean.xml` ，使用反射特性初始化 Ioc 容器，配置拦截器，控制器，最后调用 `MainSubReactorServer` 的 `run` 方法开启服务器。
3. `MainSubReactorServer` 初始化，创建若干 SubReactor 线程并开始运行。
4. 此时浏览器发来一个请求。主线程实际上是 Acceptor，通过多路复用器发现连接事件后，Acceptor 调用 `accept` 接收连接，并通过 round-robin 选择一个 subReactor 负责处理 SocketChannel。
5. subReactor 将 SocketChannel 和它的 Selector 绑定，随即注册 Read 事件。
6. `Handler` 是业务逻辑处理的核心。每个 `Handler` 对应一个 SocketChannel 的业务逻辑处理流程，通过调用 `execute` 方法，可以使得 `Handler` 进行相应事件的处理。`Handler` 内部分别维护了一个 `request` `response` `viewObject`对象，它们会在整个处理流程中被反复使用。
7. 此时浏览器发来一些数据。SubReactor 检测到 Read 事件可用。调用 `execute`，`Handler` 发现这是 Read 事件，调用 Read 处理流程。
8. 在 Read 处理流程中，分为以下部分：
   - 从 SocketChannel 读取数据，解析请求。
   - 调用拦截器组的拦截方法，对请求进行拦截。
   - 若通过拦截，检测 `request` 对应的 `Controller`。
   - 调用对应 `Controller` ，过程中可以操作 `response` 对象，可以返回 `viewObject`

9. Read 事件处理完毕后注册 Write 事件。
10. Write 事件被 SubReactor 的 selector 检测到，subReactor 调用 `Handler` 的 `execute` 方法使得 `Handler` 进行处理。
11. Write 流程主要是视图解析。视图解析包含以下内容：
    - 若 `viewObject` 是字符串，将它作为静态资源处理。
    - 若 `viewObject` 是 `HttpResponse` ，将它作为相应直接返回。
    - 若请求解析失败，返回一个 404 的响应。
12. 浏览器收到响应，展示结果。此时 SocketChannel 关闭。

### Ioc 框架的设计

1. `IocContainer` 调用 `init()` 方法，开始初始化 Ioc 容器。

以下是 `IocContainer` 类的概要：
```java
public class IocContainer {
    private final ConcurrentHashMap<String, BeanDefinition> registry = new ConcurrentHashMap<>();
    private final ArrayList<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    private String xmlPath;
    private final Object singletonLock = new Object();
    
    ...

}
```

2. 首先读取 xml。将 bean 配置文件写入 `BeanDefinition`。读取 xml 文件用到了 dom4j 的支持。这些 `BeanDefinition` 将被放入 `IocContainer` 的 `registry` 变量中。
   
   以下是 `beanDefinition` 类的概要：
   ```java
   public class BeanDefinition {
	    private String name;
	    private String classname;
	    private String initMethod;
	    private ArrayList<PropertyValue> prop = new ArrayList<>();
        private Object bean;
        
        ...
   }
   ```
2. 注册 bean 后置处理器。通过 by-type 寻找 `registry` 中类型为 `BeanPostProcessor` 的 bean 定义，然后写入 `IocContainer` 的 `beanPostProcessors` 列表。
3. 创建 bean。遍历 `registry`，依次创建 bean。若发现此 bean 有需要注入的基本类型依赖，则直接创建基本类型变量并注入。若发现此 bean 有 ref 型注入，则转向创建 ref 指向的 bean，然后再注入。（不能解决循环依赖）
4. 调用注册的 beanPostProcessor，对所有 bean 进行初始化。
5. 至此 `IocContainer` 初始化完成。

### 更新记录

- 修改线程模型，抛弃使用线程池处理。
- 增加 `JasonConverter` ，提供对 `Object` 对象的解析支持。
- 提供了包扫描的递归支持。

### 压力测试数据

JMeter 压测，测试通过接口调用 controller 业务逻辑，获取静态页面数据。测试时使用 8 SubReactor。模拟 1000 线程同时获取。

最终测试结果：253ms 平均响应速度，728ms 90%线，124ms 中位数响应速度。

### To-do

- 编写一个像 mybatis 那样的，基于 jdbc 的持久层框架。
- 对于 Ioc 的注入，提供基于注解的支持。
