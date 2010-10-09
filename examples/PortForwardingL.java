/* -*-mode:java; c-basic-offset:2; -*- */
import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PortForwardingL{
  public static void main(String[] arg){

    int lport;
    String rhost;
    int rport;

    try{
      String host=JOptionPane.showInputDialog("Enter hostname", "localhost"); 

      JSch jsch=new JSch();
      Session session=jsch.getSession(host, 22);

      String foo=JOptionPane.showInputDialog("Enter -L port:host:hostport",
					     "port:host:hostport");
      lport=Integer.parseInt(foo.substring(0, foo.indexOf(':')));
      foo=foo.substring(foo.indexOf(':')+1);
      rhost=foo.substring(0, foo.indexOf(':'));
      rport=Integer.parseInt(foo.substring(foo.indexOf(':')+1));

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);

      session.connect();

      //Channel channel=session.openChannel("shell");
      //channel.connect();

      System.out.println("localhost:"+lport+" -> "+rhost+":"+rport);

      session.setPortForwardingL(lport, rhost, rport);
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
