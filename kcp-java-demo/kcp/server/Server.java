package kcp.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import kcp.KCP;

public class Server {

    @SuppressWarnings("resource")
    public static void main(String[] args) throws IOException, Exception {
        // 创建服务器端DatagramSocket，指定端口
        DatagramSocket socket = new DatagramSocket(8800);

        // 创建kcp
        KCP kcp = new KCP(10) {
            @Override
            protected void output(byte[] buffer, int size) {
                try {
                    socket.send(new DatagramPacket(buffer, size, InetAddress.getByName("localhost"), 8808));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        kcp.SetMtu(512);

        // 接收客户端发送的数据
        System.out.println("****服务器端已经启动，等待客户端发送数据");
        while (true) {
            kcp.Update(System.currentTimeMillis());
            byte[] receMsgs = new byte[1024];
            DatagramPacket datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
            try {
                socket.receive(datagramPacket);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (datagramPacket.getLength() > 0) {
                kcp.Input(datagramPacket.getData());
            }

            byte[] buffer = new byte[1024];
            int recvlen = kcp.Recv(buffer);
            if (recvlen < 0) {
                continue;
            }
            if (recvlen > 0) {
                System.out.println(new String(Arrays.copyOf(buffer, recvlen)));
//                byte[] data2 = "欢迎您!!!!!!!!!!!!!".getBytes();
//                kcp.Send(data2);
            }
            Thread.sleep(1);
        }
    }

}
