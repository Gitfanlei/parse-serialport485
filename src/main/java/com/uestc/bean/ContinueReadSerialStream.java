package com.uestc.bean;

import java.io.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import gnu.io.*;




/*目的监听串口485输出的格雷码源数据，对数据进行转换（二进制  10进制，产生的数据以数组的形式输出
驱动模型运动）*/

/*SerialPort 继承thread 类 run 以及通过接口 implements 集成 SerialPortListener 类 中的 serialEvent方法*/

public class ContinueReadSerialStream extends Thread implements SerialPortEventListener {

    static CommPortIdentifier portIdentifier; //串口识别和管理
    static Enumeration<?> portList;             // 有效连接的串口的枚举类型变量
    InputStream inputStream;         // 将串口的数据写入输入流便于读取
    static OutputStream outputStream;   // 将向串口的输出，写入输出流，便于向串口写入   中转点是  本程序的输入与输出流
    static SerialPort serialPort1,serialPort2,serialPort3,serialPort4,serialPort5;       // 串口引用变量

    byte[] readBuffer=new byte[1024];  // 串口数据 缓存字节码数组长度为1024

    //堵塞队列用来存放读取到的数据
    private BlockingQueue<String> msgQueue=new LinkedBlockingQueue<String>();


    //    构建函数  14位（编码器分辨率）360度单圈角度转换：理解360°由分配在2^14次方的脉冲上面=每次脉冲转动的角度 (16进制输出)
    final static double unitTrans(String data) {
        // 电机分辨率
        double servoResolution = 360 / Math.pow(2, 14);

        int strLength = data.length();
        String[] dataStr = data.split("");
        StringBuilder dataSet = new StringBuilder();
        for (int i = 6; i < strLength; i++) {

            if (i <= 9) {
                dataSet.append(dataStr[i]);
            } else {
                break;
            }
        }
        System.out.println("角度值16进制数据为：" + dataSet);
        double dataInt = Integer.valueOf(new String(dataSet), 16);
        double absAngle = dataInt * servoResolution;
        return absAngle;
    }


    // 格雷码转二进制算法部分
/*    final static int grayToBinary(String grays) {
        int strLength = grays.length();
//        System.out.println("长度："+strLength);
        String[] strings=grays.split("");
        StringBuilder gray =new StringBuilder() ;
        for (int i = 1; i < strLength; i++) {
            strings[i] = Integer.valueOf(strings[i - 1]) == Integer.valueOf(strings[i]) ? "0" : "1";
        }
        for (String str : strings) {
            gray.append(str);
        }
        System.out.println("算法转换二进制数据为：" + gray.toString());
        return Integer.valueOf(new String(gray),2);
    }*/

    // 监听是串口上的数据流
    @Override
    public void serialEvent(SerialPortEvent event) {
        System.out.println("数据流类型是：" + event.getEventType());
        switch (event.getEventType()) {
            case SerialPortEvent.BI:        // 10 通讯中断
            case SerialPortEvent.FE:        // 9 帧错误
            case SerialPortEvent.PE:        // 8 奇偶校验错误
            case SerialPortEvent.OE:        // 7 溢位错误
            case SerialPortEvent.CD:        // 6 载波检测
            case SerialPortEvent.RI:        // 5 振铃指示
            case SerialPortEvent.DSR:       // 4 数据设备准备好
            case SerialPortEvent.CTS:       // 3 清除发送
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY:       // 2 输出缓存区已清空:
                break;
            case SerialPortEvent.DATA_AVAILABLE:        // 1 存在可用数据:   将串口数据读到缓存数组中 然后输出到终端     甚至可以保存数据
                try {
                    int numBytes = -1;
                    while (inputStream.available() > 0) {
                        numBytes = inputStream.read(readBuffer);        // 利用inputstream中的数据写入readbuffer,并读取readBuffer缓存字节码数组中 实际数据的长度

                        if (numBytes > 0) {
/*                            DatagramPacket datagramPacket = new DatagramPacket(readBuffer, numBytes);   // datagrampacket 对数据进行打包 数据（内容，长度）
                            readBuffer = datagramPacket.getData();*/
                            System.out.println("字节码数据内容为：" + readBuffer);
                            msgQueue.add(new Date() + "实际读取到数据为：" + new String(readBuffer));

                            String tempStr = new String(readBuffer,0,numBytes);    // 按照实际字节长度重新实例化(此实目标实际是格雷码)  构建算法实现格雷码转换

                            // 调用转换函数
                            System.out.println("当前电机实际位置为："+unitTrans(tempStr));

                            readBuffer = new byte[1024];        // 重新实例化字节码数组，避免堆溢出  实例化之后实际数据为0

                            /*// 实际使用部分*************** 本算发目标是格雷码
                            int tempInt = Integer.valueOf(tempStr, 2);
                            System.out.println("格雷码转为int:"+tempInt+"二进制表示为："+Integer.toBinaryString(tempInt));     // valueOf 方法的调用（字符串类型，源类型） integer 转换为10进制
                            System.out.println("将格雷码转为二进制为："+Integer.toBinaryString(grayToBinary(tempStr)));    // 根据元数据的格式  按位输出，向左靠齐向右补零
                            // 十进制转换
                            Integer tempInt = new Integer(tempStr);
                            System.out.println(tempInt.getClass());
                            System.out.println(Integer.toBinaryString(tempInt));
                            String tempBinary = Integer.toBinaryString(tempInt);
*/
                            // 比较存储在字节码数组中解码出来的二进制与 实际读到的格雷码转成的二进制是否相同

                        } else {
                            msgQueue.add(new Date() + "没有可读数据");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    /*返回值为0则失败，返回值为1，则成功*/
    public int openPort() {
        portList = CommPortIdentifier.getPortIdentifiers();

        while (portList.hasMoreElements()) {
            portIdentifier = (CommPortIdentifier) portList.nextElement();  // 列出枚举的下一个元素
            // 此处可以判断端口是否为串口
            System.out.println("端口类型为：" + portIdentifier.getPortType());
            System.out.println("端口名称为：" + portIdentifier.getName());

            if (portIdentifier.getName().equals("COM4")) {
                try {
                    serialPort1 = (SerialPort) portIdentifier.open("COM4", 1000);
                } catch (PortInUseException e) {
                    e.printStackTrace();
                }
                // 对串口的输入与输出进行设置
                try {
                    inputStream = serialPort1.getInputStream();
                    outputStream = serialPort1.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                    return 0;
                }
                // 增加监听器
                try {
                    serialPort1.addEventListener(this);
                } catch (TooManyListenersException e) {
                    e.printStackTrace();
                }
                // 令监听器生效
                serialPort1.notifyOnDataAvailable(true);

             /*   *********************最核心的部分设置监听器的参数，比特率，数据位，停止位，校验位等,必须与连接的串口相同，否则无法接收到数据（名称在前面已经进行判定）************* */
                try {
                    serialPort1.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.FLOWCONTROL_NONE);

                } catch (UnsupportedCommOperationException e) {
                    e.printStackTrace();
                }
            }
        }
        return 1;
    }

    // run()方法是thread类中的方法 此处重写run 所以加@override注释

    @Override
    public void run() {
        try {
            System.out.println("--------------任务处理线程运行了--------------");
            while (true) {
                // 如果堵塞队列中存在数据就将其输出  循环
                if (msgQueue.size() > 0) {
                    System.out.println(msgQueue.take());
                }
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    // 主函数入口
    public static void main(String[] args) {
        ContinueReadSerialStream continueReadSerialStream = new ContinueReadSerialStream();
        int i = continueReadSerialStream.openPort();
        if (i == 1) {
            continueReadSerialStream.start();
        } else {
            return;
        }
    }

}
