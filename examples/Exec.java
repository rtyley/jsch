/* -*-mode:java; c-basic-offset:2; -*- */
import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;

public class Exec{
  public static void main(String[] arg){

    String xhost="127.0.0.1";
    int xport=0;

    try{

      String host=JOptionPane.showInputDialog("Enter hostname", "localhost"); 
      String display=JOptionPane.showInputDialog("Enter display name", xhost+":"+xport);
      xhost=display.substring(0, display.indexOf(':'));
      xport=Integer.parseInt(display.substring(display.indexOf(':')+1));

      JSch jsch=new JSch();  
      Session session=jsch.getSession(host, 22);

      session.setX11Host(xhost);
      session.setX11Port(xport+6000);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      String command=JOptionPane.showInputDialog("Enter command", "set|grep SSH");

      Channel channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);
      channel.setXForwarding(true);

      channel.setInputStream(System.in);
      channel.setOutputStream(System.out);

      channel.connect();
    }
    catch(Exception e){
      System.out.println(e);
    }
  }

  public static class MyUserInfo implements UserInfo{
    public String getUserName(){ return username; }
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
  
    String username;
    String passwd;

    JLabel mainLabel=new JLabel("Username and Password");
    JLabel userLabel=new JLabel("Username: ");
    JLabel passwordLabel=new JLabel("Password: ");
    JTextField usernameField=new JTextField(20);
    JTextField passwordField=(JTextField)new JPasswordField(20);

    public String getPassphrase(){ return null; }
    public boolean promptNameAndPassphrase(String message){ return true; }
    public boolean promptNameAndPassword(String message){
      Object[] ob={userLabel,usernameField,passwordLabel,passwordField}; 
      int result=
	  JOptionPane.showConfirmDialog(null, ob, message,
					JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
        username=usernameField.getText();
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
