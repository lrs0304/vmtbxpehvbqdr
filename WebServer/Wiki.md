# 提供给终端的指令API

标签： Python 服务器

---
`HOST = ip地址:8080`
`API = api` 默认带上
链接格式：`HOST/API/功能`
例如 http://192.168.1.1:8080/api/power?param=shutdown

下面列出使用到的API

[TOC]

# Api-1 账号管理
```
/api/user/
```

## 用户登录
```
post `/api/user/login`
    
params:
    · account   (string) 用户账号
    · password  (string) 密码
return:
    · userId    (string) 用户Id
    · sessionId (string)
```
## 获取用户信息
```
post `/api/user/profile`

params:
    · sessionId (string)
    · userId    (string) 用户Id
return:
    · userName  (string) 用户昵称
```

# Api-2 电源开关控制
```
post `/api/power`

params:
    · object  (int), 0 for power, 1 for camera
    · type    (int), 0 for turn off, 1 for turn on
return:
    · state   (int), 1 for success
```

# Api-3 车道线检测
```
post `/api/lane-detecton`

params:
    · type    (int), 0 for turn off, 1 for turn on
return:
    · state   (int), 1 for success
```

# Api-4 实时以太网
```
post `/api/realtime-ethernet`

params:
    · type    (int), 0 for turn off, 1 for turn on
return:
    · state   (int), 1 for success
```

# Api-5 车辆控制
```
post `/api/realtime-ethernet`

params:
    · type      (int) optional, 0 for turn off, 1 for turn on
    · direction (string) optional
                    > up
                    > right-up
                    > right
                    > right-down
                    > down
                    > left-down
                    > left
                    > left-up
return:
    · state     (int), 1 for success
```

# Api-6 摄像头回传
```
待定
```




