# �ṩ���ն˵�ָ��API

��ǩ�� Python ������

---
`HOST = ip��ַ:8080`
`API = api` Ĭ�ϴ���
���Ӹ�ʽ��`HOST/API/����`
���� http://192.168.1.1:8080/api/power?param=shutdown

�����г�ʹ�õ���API

[TOC]

# Api-1 �˺Ź���
```
/api/user/
```

## �û���¼
```
post `/api/user/login`
    
params:
    �� account   (string) �û��˺�
    �� password  (string) ����
return:
    �� userId    (string) �û�Id
    �� sessionId (string)
```
## ��ȡ�û���Ϣ
```
post `/api/user/profile`

params:
    �� sessionId (string)
    �� userId    (string) �û�Id
return:
    �� userName  (string) �û��ǳ�
```

# Api-2 ��Դ���ؿ���
```
post `/api/power`

params:
    �� object  (int), 0 for power, 1 for camera
    �� type    (int), 0 for turn off, 1 for turn on
return:
    �� state   (int), 1 for success
```

# Api-3 �����߼��
```
post `/api/lane-detecton`

params:
    �� type    (int), 0 for turn off, 1 for turn on
return:
    �� state   (int), 1 for success
```

# Api-4 ʵʱ��̫��
```
post `/api/realtime-ethernet`

params:
    �� type    (int), 0 for turn off, 1 for turn on
return:
    �� state   (int), 1 for success
```

# Api-5 ��������
```
post `/api/realtime-ethernet`

params:
    �� type      (int) optional, 0 for turn off, 1 for turn on
    �� direction (string) optional
                    > up
                    > right-up
                    > right
                    > right-down
                    > down
                    > left-down
                    > left
                    > left-up
return:
    �� state     (int), 1 for success
```

# Api-6 ����ͷ�ش�
```
����
```




