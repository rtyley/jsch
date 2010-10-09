/* -*-mode:java; c-basic-offset:2; -*- */
import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class UserAuthPubKey{
  public static void main(String[] arg){

    try{
      String host=JOptionPane.showInputDialog("Enter hostname", "localhost"); 

      JSch jsch=new JSch();
      Session session=jsch.getSession(host, 22);

      //session.setUserName("username");

      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle("Choose your privatekey(ex. ~/.ssh/id_dsa)");
      chooser.setFileHidingEnabled(false);
      int returnVal = chooser.showOpenDialog(null);
      if(returnVal == JFileChooser.APPROVE_OPTION) {
        System.out.println("You chose "+
			   chooser.getSelectedFile().getAbsolutePath()+".");
        session.setIdentity(chooser.getSelectedFile().getAbsolutePath()
//			    , "passphrase"
			    );
      }

      // username and passphrase will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);
      session.connect();

      Channel channel=session.openChannel("shell");

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
    public boolean promptNameAndPassphrase(String message){
      mainLabel.setText("Please enter your user name and passphrase for "+message);
      Object[] ob={userLabel,usernameField,passwordLabel,passwordField};
      int result=
	JOptionPane.showConfirmDialog(null, ob, "username&passphrase",
				      JOptionPane.OK_CANCEL_OPTION);
      if(result==JOptionPane.OK_OPTION){
        username=usernameField.getText();
        passwd=passwordField.getText();
        return true;
      }
      else{ return false; }
    }
    public boolean promptNameAndPassword(String message){ return true; }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
  }
}


