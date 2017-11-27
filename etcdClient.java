package xxx.etcd;

import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.ClientBuilder;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Lease.KeepAliveListener;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.options.PutOption;

/**
 * etcd 客户端
 * 
 */
public class EtcdClient {
    /**
     * 日志
     */
    private static final Logger LOGGER = Logger.getLogger(EtcdClient.class);
    /**
     * etcd 请求地址
     */
    private String[] etcdUris;
    /**
     * etcd kv客户端
     */
    private KV kvClient;
    /**
     * etcd lease客户端
     */
    private Lease leaseClient;
    /**
     * etcd 租借leaseID
     */
    private long leaseID;
    /**
     * etcd keepalive 监听器
     */
    private KeepAliveListener kal;
    /**
     * 重试回调函数
     */
    private EtcdReconnectCallback cb;

    /**
     * 构造函数
     * 
     * @param etcdAddrs
     *            etcd地址信息
     */
    public EtcdClient(String etcdAddrs) {
        String[] addrs = etcdAddrs.split(",");
        for (int i = 0; i < addrs.length; i++) {
            addrs[i] = addrs[i].trim();
        }
        this.etcdUris = new String[addrs.length];
        for (int i = 0; i < addrs.length; i++) {
            this.etcdUris[i] = "http://" + addrs[i];
            LOGGER.info("etcd addrs " + etcdUris[i]);
        }
    }

    /**
     * 设置重试回调函数
     * 
     * @param cb
     *            回调接口类
     */
    public void setCb(EtcdReconnectCallback cb) {
        this.cb = cb;
    }

    /**
     * 连接etcd
     */
    public void connect() {
        Client client = ClientBuilder.newBuilder().setEndpoints(this.etcdUris).build();
        this.kvClient = client.getKVClient();
        // 设置 grant and keepalive
        this.leaseClient = client.getLeaseClient();
        try {
            this.leaseID = this.leaseClient.grant(5).get().getID();
            this.kal = this.leaseClient.keepAlive(this.leaseID);
            LeaseKeepAliveResponse lkarp = kal.listen();
            LOGGER.info("listen LeaseKeepAliveResponse ttl is : " + lkarp.getTTL());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("etcd grant failed. will exit." + e.toString());
            System.exit(0);
        }
        keepalive();
    }

    /**
     * etcd 连接 keepalive
     */
    private void keepalive() {
        // 检查lease keepalive
        long timeInterval = 1000;
        Runnable runnable = new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(timeInterval);
                        LeaseKeepAliveResponse lkarp = kal.listen();
                        LOGGER.debug("listen LeaseKeepAliveResponse ttl is : " + lkarp.getTTL());
                    } catch (Exception e) {
                        LOGGER.error("etcd keepalive exception : " + e.toString());
                        try {
                            leaseID = leaseClient.grant(5).get().getID();
                        } catch (InterruptedException | ExecutionException e1) {
                            LOGGER.error("etcd grant failed. will retry." + e1.toString());
                            continue;
                        }
                        kal = leaseClient.keepAlive(leaseID);
                        cb.callback();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * 获取配置
     * 
     * @param key
     *            配置key
     * @return 配置值
     * @throws InterruptedException
     *             被中断异常
     * @throws ExecutionException
     *             执行异常
     */
    public String get(String key) throws InterruptedException, ExecutionException {
        GetResponse res = kvClient.get(ByteSequence.fromString(key)).get();
        if (res.getCount() == 0) {
            return "";
        }
        return res.getKvs().get(0).getValue().toStringUtf8();
    }

    /**
     * 设置配置
     * 
     * @param key
     *            配置key
     * @param value
     *            配置值
     * @throws InterruptedException
     *             被中断异常
     * @throws ExecutionException
     *             执行异常
     */
    public void put(String key, String value) throws InterruptedException, ExecutionException {
        kvClient.put(ByteSequence.fromString(key), ByteSequence.fromString(value),
                PutOption.newBuilder().withLeaseId(leaseID).build()).get();
    }

    /**
     * 删除配置
     * 
     * @param key
     *            配置项
     * @throws InterruptedException
     *             被中断异常
     * @throws ExecutionException
     *             执行异常
     */
    public void del(String key) throws InterruptedException, ExecutionException {
        kvClient.delete(ByteSequence.fromString(key)).get();
    }

}
