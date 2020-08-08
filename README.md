# 基于Reactor架构的web服务器及其web开发框架

> 一个用原生 Java 实现的 NIO http服务器，模仿 Springboot 和 SpringMVC，实现了请求解析，controller，interceptor，视图解析等基本功能。**不需要用到任何第三方包。**
>
> 作者：@ Galaxyzeta

## 摘要

作者 @Galaxyzeta 做过一段时间的 web 开发，熟悉 SpringBoot，SpringMVC，对其原理有了一定的了解。突然有一天，憨憨作者突发奇想，要做个服务器玩玩，服务器做到一半，作者意外发现服务器设计的可拓展性较强，可以额外再此基础上做个 Web 服务框架，于是就有了这个东西。

项目的服务器设计采用基础 Reactor 架构，框架的设计参考了 Spring MVC，SPring Boot 的部分设计，实现方法可能不同，但原理就那么回事。

只能算是个玩具级项目，如果想拿来研究一下服务器原理，Mvc 原理，作者@Galaxyzeta 表示十分欢迎。

**如果这个项目对你有帮助，给作者一个 Star 吧**

关键词：**Reactor 架构** | **Web开发框架** | **NIO编程** | **反射** | **线程池**



## 快速上手

项目非常容易使用，简单来说有以下几步：

1. 设置配置文件。

2. 编写控制器和拦截器。

3. 启动服务器。

我以一个简单的项目 Demo 来演示一下项目的基本功能和使用方法：

项目结构：

```txt
+ src
|___+ com
    |___ ... ... 这里是框架和服务器代码
|___+ test
    |___+ controller
        |___MyController.java
    |___+ interceptor
        |___MyInterceptor1.java
        |___MyInterceptor2.java
    |___+ static
        |___+ html
            |___index.html
        |___+ css
            |___mycss.css
        |___+ javascript
            |___myjs.js
    |___TestAPI.java

```

### 服务启动

首先你需要一个启动整个应用程序的类，在这里他是 `TestAPI.java` 。

```java
package test;

import com.galaxyzeta.server.reactor.WebApplicationContext;

public class TestAPI {
    public static void main(String[] args) {
        WebApplicationContext.runApplication("src/test/config.property");
    }
}
```

很简单，调用 `WebApplicationContext.runApplication(String configPath)` 就能启动项目。其中，参数是配置文件相对当前工作目录的相对地址，或指定一个绝对地址。

### 配置文件

看一下配置文件：

```txt
port: 8080
controller-base-package: test/controller
interceptor-base-pacakge: test/interceptor
interceptors: MyInterceptor1, MyInterceptor2
static: src/test/static
```

参数说明：

**port**: 服务器运行的端口

**controller-base-package**: Controller 所在的包，包中放置控制器，所谓控制器，就是负责业务逻辑处理的部分。

**interceptor-base-package**: Interceptor 所在的包，包中放置拦截器，所谓拦截器，就是拦截请求，并对其进行校验，决定是否让它进入业务逻辑处理的 java 类。

**interceptors**: 用逗号分割，指定了全部用于请求过滤的拦截器，分先后顺序，前面的拦截器将最先执行。

**static**: 静态资源存放根目录。

### 控制器

然后让我们写一个简单的控制器，`MyController.java`

```java
package test.controller;

import com.galaxyzeta.annotation.RequestMapping;
import com.galaxyzeta.http.HttpRequest;
import com.galaxyzeta.http.HttpResponse;
import com.galaxyzeta.server.reactor.Controller;
import com.galaxyzeta.util.Logger;
import com.galaxyzeta.util.LoggerFactory;

public class MyController implements Controller {

    // 从 LoggerFactory 取得一个 Logger，用于日志记录
    private static Logger LOG = LoggerFactory.getLogger(MyController.class);

    @RequestMapping(method = "GET", url = "/debug")
    public static Object debugGet(HttpRequest req, HttpResponse resp) {
        LOG.DEBUG("GET /debug invoked OK");
        return "html/index.html";
    }
}
```

简单的解释一下这个控制器，这个控制器包含一个处理 GET /debug 请求的方法，方法运行时，先输出一段日志，然后返回 `html/index.html` 这个**视图对象**。

说明：

- 控制器必须放在配置文件指定的包下，且不能是包内的子包。

- 控制器的设计和 Spring Boot 十分相似，作为控制器的方法必须附带 `@RequestMapping(method = "xxx", url = "xxx")`，一个 Controller 实现类中可以附带多个业务逻辑方法。

- 项目对业务逻辑方法的限制：必须是 `public static`，参数必须有 `HttpReqeust` 和 `HttpResponse`。

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

这两个拦截器什么都没干，只是输出日志，并返回 `true`，返回 `true` 的含义是放行，请求将前往下一个拦截器接受检查。当然你可以对 request 进行检查，禁止某些 request 通过拦截器，只需返回  `false` 即可。

根据配置文件，MyInterceptor1 首先作用，而 MyInterceptor2 将在请求通过前置拦截器的检查后发挥作用。

说明：

- 拦截器必须放在指定的包下。

- 拦截器的方法规范：必须是 `public static boolean intercept(HttpRequest req, HttpResponse resp)` 不能有任何变动。

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

![Reactor Server](https://github.com/Galaxyzeta/pictureBed/blob/master/ReactorServerAndMVC.png?raw=true)

核心类有两个：

`ReactorServer` 是服务器主体，Epoll 模型写在这里，这个类有一个 dispatch 方法处理被 epoll 方法选中的 Channel，它通过调用 Channel 附件 handler 的 run 方法，进入 handler 处理流程；

`Handler` 类负责完整的流程处理，包含请求解析，请求拦截，请求处理，视图解析，响应返回整个流程处理。它重点维护三个对象，response，request，viewObject。一个 SocketChannel 将附带一个 handler 对象。

1. 浏览器发起请求到达 ReactorServer。Selector（图中的Multiplexing，MUX）检测到 OP_ACCEPT 事件可用，故接收一个 TCP 连接。此过程生成一个绑定了 handler 的 SocketChannel，然后把它注册到 Selector 上，事件定义为 OP_READ（可读）。

2. Selector 检测到 OP_READ 事件就绪，选出可用的 Channel 并通过 dispatch 调用 run 方法。run 方法中判断其状态为 READ，故执行 read() 方法。

3. read() 方法解析 request，把它变成一个 HttpRequest 对象，存储在 handler 的成员属性中。此时所有读取已经完成，对其 shutdown input 避免 epoll 时再次被选中。

4. 随后 read() 调用 process()，在 process 方法中，业务逻辑被封装成一个内部类，把它扔给线程池处理。具体来说，业务逻辑包含了 Interceptor 拦截和 Controller 业务处理。

5. 线程池完成处理后，将 handler 内置状态标记为 SEND，同时将 Channel 的事件改为 OP_WRITE（可写）。

6. 视图解析。根据 handler 维护的 viewObject 类型判断如何填写 response。若 Controller 返回的 Object 是 String，试图进行资源解析；若返回值类型为 HttpResponse，则不用解析。

7. 发回 client，shutdown output 并关闭 socketChannel。至此一次传输完成，浏览器应显示结果。

   

### ReactorServer 启动流程

启动流程包含了对配置文件的扫描，Controller 包扫描，Interceptor 注册。用到了反射技术。

所有的初始化过程全部放在 `WebApplicationContext` 完成，具体内容请看项目吧。



## Todo List

一些可能在将来加入的功能。敬请期待！

1. 增加控制反转和依赖注入容器。
2. 增加 POST 方法的更便捷支持。
3. 增加对 xml，yml等文件的解析支持。
4. 增加模板引擎功能，并新增模板引擎视图解析。
5. 服务器进一步优化。