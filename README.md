# Gomoku
HITSZ 2024 分布式系统大作业 五子棋
## 本项目使用环境配置
操作系统 Windows 11
tomcat 版本 Apache Tomcat/9.0.89
Maven 版本 apache-maven-3.9.6
```powershell
mvn clean install # 构建本项目
# 将构建文件夹 target/gomoku copy 到 tomcat webapp 目录下
catalina start  # 开启 tomcat 服务器
# 在浏览器中打开 http://localhost:8080/gomoku/game.html 即可开始对战
```
## 如何实现通讯
### WebSocket 实现配对
client 序列号进行匹配：0-1，2-3，4-5......
### 消息同步
1. 开始游戏
   1. 传递名字与开始请求到服务器，服务器传回空棋盘
   2. 有新的 client 加入，匹配成功，服务器建立对战号
   3. 而后的操作均由对战号确定消息同步需要通知的棋盘
2. 更新信息
```json
// 客户端 -> 服务器
{                 
  "msgKind":"move",               // 修改棋盘
  "moveKind":"forward"/"backward",  // 下 or 悔棋
  "place":{"row":1,"col":2}       // 表示点击改变的棋盘位置
}

{
  "msgKind":"start",               // 开始游戏 
  "name":"xxx"                     // 输入用户名
}
// 客户端只能发起改变棋盘状态的请求，只有服务器发到客户端的 msg 才能改变客户端棋盘状态

// 服务器 -> 客户端
{
  "msgKind":"move",               // 更新棋盘
  "moveKind":"forward"/"backward",
  "place":{"row":1,"col":2},
  "color":"X"/"O"                 // 需要黑棋 or 白棋
} 
{
  "msgKind":"start",                // 开始游戏 
  "name":"xxx",                     // 用户名1
  "opName":"yyy"                    // 用户名2
}
{
  "msgKind":"wait",                 // 等待你的对手下棋
}
{
  "msgKind":"win",                 // 一方取得胜利
}
{
  "msgKind":"lose",                 // 一方失败
}
{
  "msgKind":"regret",                 // 一方悔棋
}
```
