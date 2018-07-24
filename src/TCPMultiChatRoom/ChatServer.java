package TCPMultiChatRoom;


import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;

//import TCPMultiChatRoom2.ChatServer.ClientServiceThread;
import net.sf.json.JSONObject;

public class ChatServer extends JFrame {
 
  private static final long serialVersionUID = 1L;
  // 服务器启动的时候新建一个聊天组
  //这是一个用户信息列表
  ArrayList<User> clientList = new ArrayList<User>();
  //这是在线用户名列表
  ArrayList<String> usernamelist = new ArrayList<String>();
  //创建一个信息显示框
  private JTextArea jta = new JTextArea();
  //声明一个用户对象，该类里面有两个变量 socket，username；
  private User user = null;
  //声明一个输出流
  DataOutputStream output = null;
  //声明一个输入流
  DataInputStream input = null;
  
  private JTextField serverMessageTextField;
  private JButton sendButton;
  private JPanel messagePanel;
  
  public static void main(String[] args) {
    new ChatServer();
  }

  public ChatServer() {
    // 设置信息显示框版面
    setLayout(new BorderLayout());
    add(new JScrollPane(jta), BorderLayout.CENTER);
    jta.setEditable(false);
    setTitle("多人聊天室服务器端 ");
    setSize(500,300);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setVisible(true); // 

    
    /*
  //server log info
  	logTextArea = new JTextArea();
  	logTextArea.setEditable(false);
  	logTextArea.setForeground(Color.blue);

  	logPanel = new JScrollPane(logTextArea);
    */
    
    
  //server message
  	serverMessageTextField = new JTextField();
  	sendButton = new JButton("广播");

  	messagePanel = new JPanel(new BorderLayout());
  	messagePanel.add(serverMessageTextField, "Center");
  	messagePanel.add(sendButton, "East");
  	messagePanel.setBorder(new TitledBorder("广播消息"));
    
  	//add(logPanel, "Center" );
  	add(messagePanel, "South");
    
  	addActionListenersToUI();
    
    
    
    
    
    
    
    
    
    
    try {
      // 创建一个服务器socket，绑定端口8000
      ServerSocket serverSocket = new ServerSocket(8888);
      //打印启动时间
      jta.append("服务器启动时间 " + new Date() + '\n');
      //logTextArea.append("服务器启动时间 " + new Date()+ "\r\n");
     //无限循环监听是否有新的客户端连接
      while (true) {
        // 监听一个新的连接
        Socket socket = serverSocket.accept();
        if(socket!=null){
        	//获取上线用户的信息
            input = new DataInputStream(socket.getInputStream());
            String json = input.readUTF();
            JSONObject data = JSONObject.fromObject(json.toString());
            System.out.println("###"+data.getString("username"));
            jta.append("用户:" + data.getString("username") +
              "在" + new Date()+"登陆系统" + '\n');
           //显示用户登录ip地址
            InetAddress inetAddress = socket.getInetAddress();
            jta.append("用户" + data.getString("username") + "的IP地址是："
              + inetAddress.getHostAddress() + "\n");
           //新建一个用户对象
            user = new User();
            //设置该用户对象的socket
        	user.setSocket(socket);
        	//设置用户名
        	user.setUserName(data.getString("username"));
        	//加入在线用户组列表
        	clientList.add(user);
        	//加入用户名列表（用户显示在客户端的用户列表）
        	usernamelist.add(data.getString("username"));
        }
        //用户上线提示，打包成json格式数据
        JSONObject online = new JSONObject();
        online.put("userlist", usernamelist);
        online.put("msg", user.getUserName()+"上线了");
        
        //提示所有用户有新的用户上线
        for (int i = 0; i < clientList.size(); i++) {
        	try{
            User otheruser = clientList.get(i);
            //获取每一个用户的socket，得到输出流，
                output = new DataOutputStream(otheruser.getSocket().getOutputStream());
            //向每个用户端发送数据
                output.writeUTF(online.toString());
        	}catch(IOException ex){
        		System.err.println(ex);
        	}
        	}
        //新开一个线程，并将当前连接用户的socket传给这个线程，该线程用于负责监听该socket的数据
        HandleAClient task = new HandleAClient(socket);
        new Thread(task).start();

      }
    }catch(IOException ex) {
      System.err.println(ex);
    }
  }

  private void addActionListenersToUI() {
	// TODO 自动生成的方法存根
	  sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sendAll();
			}
		});
}

protected void sendAll() {
	// TODO 自动生成的方法存根
	String message = serverMessageTextField.getText().trim();
	if (message == null || message.equals("")) {
		showErrorMessage("发送消息不能为空！");
		return;
	}

	
	// 设置日期格式
	  SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
    String time =df.format(new Date()).toString();
	
    JSONObject mes = new JSONObject();
    //mes.put("userlist", usernamelist);
    mes.put("msg", "系统通知 " +time+'\n'+message.toString());
    
    //发送广播
    for (int i = 0; i < clientList.size(); i++) {
    	try{
        User otheruser = clientList.get(i);
        //获取每一个用户的socket，得到输出流，
            output = new DataOutputStream(otheruser.getSocket().getOutputStream());
        //向每个用户端发送数据
            output.writeUTF(mes.toString());
    	}catch(IOException ex){
    		System.err.println(ex);
    	}
    	}
	
	jta.append(("Server: " + message+ "\r\n"));
	
	
	/*for (Map.Entry<String, HandleAClient> entry : HandleAClient.entrySet()) {
		entry.getValue().sendMessage("MSG@ALL@SERVER@" + message);
	}*/

	//logMessage("Server: " + message);
	serverMessageTextField.setText(null);
}

private void showErrorMessage(String msg) {
	JOptionPane.showMessageDialog(jta, msg, "Error", JOptionPane.ERROR_MESSAGE);
}



// 线程类
  class HandleAClient implements Runnable {
    private Socket socket; //已连接的cocket

    public HandleAClient(Socket socket) {
      this.socket = socket;
    }

    public void run() {
    	
      try {
        // 获取本线程监听的socket客户端的输入流
        DataInputStream inputFromClient = new DataInputStream(
          socket.getInputStream());

        // 循环监听
        while (true) {
          // 获取客户端的数据
          String json = inputFromClient.readUTF();

          JSONObject data = JSONObject.fromObject(json.toString());
          //将获取的数据转发给每一个用户
          for (int i = 0; i < clientList.size(); ) {
        	  try{
        		  //将聊天的信息和用户列表打包成json格式数据发给每个客户端
        		  JSONObject chat = new JSONObject();
                  chat.put("userlist", usernamelist);
                  chat.put("msg", data.getString("username")+" "+data.getString("time")+":\n"+data.getString("msg"));
                  User otheruser = clientList.get(i);
                  output = new DataOutputStream(otheruser.getSocket().getOutputStream());
                  output.writeUTF(chat.toString());
                  i++;
        	  }catch(SocketException ex){
        		  //如果出现异常，表明当前循环的客户端下线了
        		  //从列表中移除
        		  User outuser = clientList.get(i);
        		  clientList.remove(i);
        		  //提示每个用户有用户下线了
        		  usernamelist.remove(outuser.getUserName());
        		  JSONObject out = new JSONObject();
                  out.put("userlist", usernamelist);
                  out.put("msg", outuser.getUserName()+"下线了\n");
        		  //通知下线
                  //日志记录下线用户
                  jta.append("用户:" + outuser.getUserName() +
                          "在" + new Date()+"退出系统" + '\n');
                  for (int j = 0; j < clientList.size(); j++) {
                	  try{
                      User otheruser = clientList.get(j);
                          output = new DataOutputStream(otheruser.getSocket().getOutputStream());
                          output.writeUTF(out.toString());
                	  }catch(IOException ex1){
                  	  }
                  }
          	}
          }
        }
      }
      catch(IOException e) {
        System.err.println(e);
      }
    }
  }
}
