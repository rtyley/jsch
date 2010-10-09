import com.jcraft.jsch.*;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ScpTo{
  public static void main(String[] arg){
    if(arg.length!=2){
      System.err.println("usage: java ScpTo file1 remotehost:file2");
      System.exit(-1);
    }      

    try{

      String lfile=arg[0];
      String host=arg[1].substring(0, arg[1].indexOf(':'));
      String rfile=arg[1].substring(arg[1].indexOf(':')+1);

      JSch jsch=new JSch();
      Session session=jsch.getSession(host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();


      // exec 'scp -t rfile' remotely
      String command="scp -t "+rfile;
      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // plug I/O streams for remote scp
      PipedOutputStream out=new PipedOutputStream();
      channel.setInputStream(new PipedInputStream(out));
      PipedInputStream in=new PipedInputStream();
      channel.setOutputStream(new PipedOutputStream(in));

      channel.connect();

      byte[] tmp=new byte[1];

      // wait for '\0'
      do{ in.read(tmp, 0, 1); }while(tmp[0]!=0);

      // send "C0644 filesize filename", where filename should not include '/'
      int filesize=(int)(new File(lfile)).length();
      command="C0644 "+filesize+" ";
      if(lfile.lastIndexOf('/')>0){
        command+=lfile.substring(lfile.lastIndexOf('/')+1);
      }
      else{
        command+=lfile;
      }
      command+="\n";
      out.write(command.getBytes()); out.flush();

      // wait for '\0'
      do{ in.read(tmp, 0, 1); }while(tmp[0]!=0);

      // send a content of lfile
      FileInputStream fis=new FileInputStream(lfile);
      byte[] buf=new byte[1024];
      while(true){
        int len=fis.read(buf, 0, buf.length);
	if(len<=0) break;
        out.write(buf, 0, len); out.flush();
      }

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      // wait for '\0'
      do{ in.read(tmp, 0, 1); }while(tmp[0]!=0);

      System.exit(0);
    }
    catch(Exception e){
      System.out.println(e);
    }
  }
  public static class MyUserInfo implements UserInfo{
    public String getName(){
      passwordDialog.show();
      passwd=new String(passwordField.getPassword());
      username=usernameField.getText();
      passwordField.setText("");
      if(passwd.length()==0)passwd=null;
      return username;
    }
    public String getPassword(){ 
      if(passwd==null){
        passwordDialog.show();
        passwd=new String(passwordField.getPassword());
        username =usernameField.getText();
        passwordField.setText("");
      } 
      return passwd;
    }
  
    public boolean prompt(String str){
      Object[] options={ "yes", "no" };
      int foo=JOptionPane.showOptionDialog(null, 
             str,
             "Warning", 
             JOptionPane.DEFAULT_OPTION, 
             JOptionPane.WARNING_MESSAGE,
             null, options, options[0]);
       return foo==0;
    }
  
    public boolean retry(){ 
      passwd=null;
      passwordField.setText("");
      return true;
    }
  
    String username;
    String passwd;
    final JDialog passwordDialog=new JDialog(new JFrame(), true);  
    JLabel mainLabel=new JLabel("Please enter your user name and password: ");
    JLabel userLabel = new JLabel("User name: ");
    JLabel passwordLabel = new JLabel("Password: ");
    JTextField usernameField = new JTextField(20);
    JPasswordField passwordField = new JPasswordField(20);
    JButton okButton = new JButton("OK");
  
    MyUserInfo(){
      Container pane = passwordDialog.getContentPane();
      pane.setLayout(new GridLayout(4, 1));
      pane.add(mainLabel);
      JPanel p2 = new JPanel();
      p2.add(userLabel);
      p2.add(usernameField);
      usernameField.setText(username);
      pane.add(p2);
      JPanel p3 = new JPanel();
      p3.add(passwordLabel);
      p3.add(passwordField);
      pane.add(p3);
      JPanel p4 = new JPanel();
      p4.add(okButton);
      pane.add(p4);   
      passwordDialog.pack();
      ActionListener al=new ActionListener(){
        public void actionPerformed(ActionEvent e) { passwordDialog.hide(); } 
      };
      okButton.addActionListener(al);
      passwordField.addActionListener(al);
    }
    public String getPassphrase(String message){ return null; }
  }
}
