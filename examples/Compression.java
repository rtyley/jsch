import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Compression{
  public static void main(String[] arg){

    try{
      String host=JOptionPane.showInputDialog("Enter hostname", "localhost"); 

      JSch jsch=new JSch();
      Session session=jsch.getSession(host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      java.util.Properties config=new java.util.Properties();
      config.put("compression.s2c", "zlib");
    //config.put("compression.c2s", "zlib");
      session.setConfig(config);
      session.connect();

      Channel channel=session.openChannel("shell");
      channel.connect();
    }
    catch(Exception e){
      System.out.println(e);
    }
  }
  public static class MyUserInfo implements UserInfo{
    public String getName(){ return username; }
    public String getPassword(){ return passwd; }
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
    JLabel mainLabel=new JLabel("Username and Password");
    JLabel userLabel=new JLabel("Username: ");
    JLabel passwordLabel=new JLabel("Password: ");
    JTextField usernameField=new JTextField(20);
    JTextField passwordField=(JTextField)new JPasswordField(20);
  
    MyUserInfo(){ }

    public String getPassphrase(String message){ return null; }
    public boolean promptNameAndPassphrase(String message){ return true; }
    public boolean promptNameAndPassword(String message){
      Object[] ob={userLabel,usernameField,passwordLabel,passwordField}; 
      int result=
	  JOptionPane.showConfirmDialog(null, ob, "username&passwd", 
					JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
        username=usernameField.getText();
	passwd=passwordField.getText();
	return true;
      }
      else{ return false; }
    }
  }
}


