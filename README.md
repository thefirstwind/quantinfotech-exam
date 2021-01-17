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
具体业务应该是证券相关的策略操作相关的业务。

单就程序来讲
要测试的目标地址统一保存在 config/strategy.properties 文件中
config/strategy.properties 文件内容如下
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
### 1.3 改进意见
* 在代码中 使用TODO标签，标明了修改意见
* 另外，代码可以进一步的抽离共通方法
## 2
## 3