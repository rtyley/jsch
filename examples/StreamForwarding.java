import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class StreamForwarding{
  public static void main(String[] arg){
    String host;
    int port;

    try{
      host=JOptionPane.showInputDialog("Enter hostname", "localhost"); 

      JSch jsch=new JSch();
      Session session=jsch.getSession(host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      String foo=JOptionPane.showInputDialog("Please enter host and port", 
						 "host:port");
      host=foo.substring(0, foo.indexOf(':'));
      port=Integer.parseInt(foo.substring(foo.indexOf(':')+1));

      System.out.println("System.{in,out} will be forwarded to "+
			 host+":"+port+".");
      Channel channel=session.openChannel("direct-tcpip");
      ((ChannelDirectTCPIP)channel).setInputStream(System.in);
      ((ChannelDirectTCPIP)channel).setOutputStream(System.out);
      ((ChannelDirectTCPIP)channel).setHost(host);
      ((ChannelDirectTCPIP)channel).setPort(port);
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


