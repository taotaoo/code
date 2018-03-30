package kcp.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import kcp.KCP;

public class Client {

    public static void main(String[] args) throws Exception {
        // 创建DatagramSocket对象
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(8808);
            socket.connect(InetAddress.getByName("localhost"), 8800);
            socket.setSoTimeout(1);
        } catch (SocketException | UnknownHostException e1) {
            e1.printStackTrace();
            return;
        }
        // 创建kcp
        KCP kcp = new KCP(10) {
            @Override
            protected void output(byte[] buffer, int size) {
                try {
                    socket.send(new DatagramPacket(buffer, size));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        kcp.SetMtu(512);

        int len = 100000;
        byte[] data = new byte[len];
        for (int i = 0; i < len; i++) {
            data[i] = 'b';
        }
        kcp.Send(data);
        byte[] receMsgs = new byte[1024];

        while (true) {
            kcp.Update(System.currentTimeMillis());

            DatagramPacket datagramPacket = new DatagramPacket(receMsgs, receMsgs.length);
            try {
                socket.receive(datagramPacket);
            } catch (IOException e1) {
                Thread.sleep(1);
                continue;
            }
            kcp.Input(datagramPacket.getData());

            byte[] buffer = new byte[1024];
            int recvlen = kcp.Recv(buffer);
            if (recvlen < 0) {
                continue;
            }
            if (recvlen > 0) {
                System.out.println(new String(Arrays.copyOf(buffer, recvlen)));
                // kcp.Send("123456789012345678901234567890".getBytes());
            }
            Thread.sleep(1);
        }
    }

}
