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
* 同时出现tab缩进 和空格缩进
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
* 统计单次出现的次数

详细设计:
* 列出文件列表
* 忽略大小写，去掉除英文单词和空格以外所有其他字符，使用空格分隔
* 考虑到程序有可能中途中断，需要使用，文件列表控制统计进度
* 4核8G的机器，文件单独大小以10M为上线，同时处理800个线程应该是可以的，系统保守分配500个线程
* 另外500个线程同时开放，2T文件 执行队列size设置为400
* 每一个文件统计时间假设为1s，那么整体执行结束预估400s，7分钟不到
* 常用英文单次4000~8000左右，可以使用一个大HashMap<key,count> 结构存储所有单词 和 出现的次数
* 最后根据800个线程统计出来的数据，合并到一起，放到那个大HashMap里

以上是理论值，以下模拟

## 3