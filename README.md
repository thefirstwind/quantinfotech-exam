# quantinfotech-exam

##
3道题，相关资源分别在以下3个目录里
```
-
 + exam01
 + exam02
 + exam03
```

## 1 StrategyOperation的code review
### 1.1 评估代码风格的问题
* 缩进不一致，多人多次修改提案该代码，风格不一
* 同时出现tab缩进 和空格缩进单词
* 没用的注释太多，如果没有实际意义可以删除掉

### 1.2 代码的意图

具体业务应该是证券相关的策略操作相关的业务。基本上表意清晰。

单就程序来讲
要测试的目标地址统一保存在 config/strategy.properties 文件中

config/strategy.properties
``` properties
## 登陆
strategy.login.path: 
## 登出
strategy.logout.path:
## 开始
strategy.start.path:
## 停止
strategy.stop.path:
## 更新
strategy.update.path:
## 添加
strategy.add.path
## 查找
strategy.find.path
## 上传
strategy.upLoad.path
## 代下单
manual.placeOrder.path
## 代取消
manual.cancelOrder.path
## 风险
strategy.risk.path
## 查找风险
risk.find.path
## 查找某用户的策略操作
strategy.findAlgo.path
```

config 对象中有 caseMap、cookieMap、golbalMap
```
caseMap保存当前登陆用户
cookieMap 保存cookie中的信息
globalMap 保存的信息有：instanceId，risk
```
### 1.3 改进意见
* 在代码中 使用TODO标签，标明了修改意见
* 另外，代码可以进一步的抽离共通方法，如 login logout start update stop 方法，相同的操作很多，可以提取通用操作
* 就测试代码来说推荐使用junit框架测试mock数据。使用断言实现测试自动化。当前代码只能通过输出结果进行判断，扩展难，执行需要依赖其他环境。不过如果是黑盒测试的话，基本上能完成测试目的。

## 2
题目关键词：
* 4核8G Linux
* 文件数量不定
* 文件总大小2T
* 单个文件大小 几百k~几兆
* 要最快的统计
* 统计单词出现的次数

详细设计:
* 列出文件列表
* 忽略大小写，去掉除英文单词和空格以外所有其他字符，使用空格分隔
* 常用英文单词4000~8000左右，可以使用一个大HashMap<key,count> 结构存储所有单词 和 出现的次数
* 最后根据n个线程统计出来的数据，合并到一起，放到那个大HashMap里

程序：
https://github.com/thefirstwind/quantinfotech-exam/tree/main/exam02/src/main/java/com/quantinfotech/exam02

说明：
* 分别使用2中方法实现，
* 测试样本数据为7.7M和 123M。
* 7.7M样本数据，速度相差不大。
* 123M样本数据，单线程和多线程之间，效率相差将近1倍以上。
* 另外即使最快的方法，预估执行完2T的数据，也要至少6天的时间。

评估方法：
* 123M 需要 16s
* 2T大小的文件需要多少时间
$$  
\frac{2T}{2T所需时间} = \frac{123M}{16s}
$$

2T所需的时间是=2*1024*1024 ÷ 123 x 16s =75h



所以 后期的优化，考虑使用多实例同时读取文件，但是这样的话，就需要用其他别的方式实现。
时间原因，只能说一下思路。 列出所有文件，用文件列表控制统计进度，  以物理文件形式保存（或者使用redis之类保存）
最后对n个文件的统计结果进行统合。

## 3