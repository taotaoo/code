# 注意
* kcpClient同kcpServer开始交互的时候会维护一个发送数据包的序号，server对该序号之前的数据都做去重处理。
* 故在client重启，但是server未重启的场景下，是不能正常工作的。
