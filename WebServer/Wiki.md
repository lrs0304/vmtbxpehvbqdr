# 提供给终端的指令API

标签： Python 服务器

---
`HOST = ip地址:8080`<br />
`API = api` 默认带上<br />
链接格式：`HOST/API/功能`<br />
例如 http://192.168.1.1:8080/api/power?param=shutdown

下面列出使用到的API
* [Api-1 账号管理](#api1)
    * [用户登录](#api1-user)
    * [获取用户信息](#api1-profile)
* [Api-2 电源开关控制](#api2)
* [Api-3 车道线检测](#api3)
* [Api-4 实时以太网](#api4)
* [Api-5 车辆控制](#api5)
* [Api-6 摄像头回传](#api6)

# <a name="api1"/>Api-1 账号管理
```
/api/user/
```

## <a name="api1-user"/>用户登录
```
post `/api/user/login`
    
params:
    · account   (string) 用户账号
    · password  (string) 密码
return:
    · userId    (string) 用户Id
    · sessionId (string)
```
## <a name="api1-profile"/>获取用户信息
```
post `/api/user/profile`

params:
    · sessionId (string)
    · userId    (string) 用户Id
return:
    · userName  (string) 用户昵称
```

# <a name="api2"/>Api-2 电源开关控制
```
post `/api/power`

params:
    · object  (int), 0 for power, 1 for camera
    · type    (int), 0 for turn off, 1 for turn on
return:
    · state   (int), 1 for success
```

# <a name="api3"/>Api-3 车道线检测
```
post `/api/lanedetecton`

params:
    · type    (int), 0 for turn off, 1 for turn on
return:
    · state   (int), 1 for success
```

# <a name="api4"/>Api-4 实时以太网
```
post `/api/realtimeethernet`

params:
    · type    (int), 0 for turn off, 1 for turn on
return:
    · state   (int), 1 for success
```

# <a name="api5"/>Api-5 车辆控制
```
post `/api/carcontrol`

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

# <a name="api6"/>Api-6 摄像头回传
```
待定
```




