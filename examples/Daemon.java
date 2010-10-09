/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import com.jcraft.jsch.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Daemon{
  public static void main(String[] arg){

    int rport;
    String classname;

    try{
      JSch jsch=new JSch();

      String host=JOptionPane.showInputDialog("Enter username@hostname",
                                              System.getProperty("user.name")+
                                              "@localhost"); 
      String user=host.substring(0, host.indexOf('@'));
      host=host.substring(host.indexOf('@')+1);

      Session session=jsch.getSession(user, host, 22);

      String foo=JOptionPane.showInputDialog("Enter remote port number", 
                                             "8888");
      rport=Integer.parseInt(foo);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);

      session.connect();

      //session.setPortForwardingR(rport, Parrot.class.getName());
      session.setPortForwardingR(rport, "Daemon$Parrot");
      System.out.println(host+":"+rport+" <--> "+"Parrot");
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public static class Parrot implements ForwardedTCPIPDaemon{
    ChannelForwardedTCPIP channel;
    Object[] arg;
    public void setChannel(ChannelForwardedTCPIP c){this.channel=c;}
    public void setArg(Object[] arg){this.arg=arg;}
    public void run(){
      System.out.println("remote port: "+channel.getRemotePort());
      System.out.println("remote host: "+channel.getSession().getHost());
      try{
        InputStream in=channel.getInputStream();
        OutputStream out=channel.getOutputStream();
        byte[] buf=new byte[1024];
        while(true){
          int i=in.read(buf, 0, buf.length);
          if(i<=0)break;
          out.write(buf, 0, i);
          if(buf[0]=='.')break;
        }
      }
      catch(IOException e){
      }
    }
  }

  public static class MyUserInfo implements UserInfo{
    public String getPassword(){ return passwd; }
    public boolean promptYesNo(String str){
      Object[] options={ "yes", "no" };
      int foo=JOptionPane.showOptionDialog(null, 
             str,
             "Warning", 
             JOptionPane.DEFAULT_OPTION, 
             JOptionPane.WARNING_MESSAGE,
             null, options, options[0]);
       return foo==0;
    }
  
    String passwd;
    JTextField passwordField=(JTextField)new JPasswordField(20);

    public String getPassphrase(){ return null; }
    public boolean promptPassphrase(String message){ return true; }
    public boolean promptPassword(String message){
      Object[] ob={passwordField}; 
      int result=JOptionPane.showConfirmDialog(null, ob, message,
                                               JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
        passwd=passwordField.getText();
        return true;
      }
      else{ return false; }
    }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
  }
}
