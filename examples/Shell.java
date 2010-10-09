/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
import com.jcraft.jsch.*;
import javax.swing.*;

public class Shell{
  public static void main(String[] arg){
    
    try{
      JSch jsch=new JSch();

      //jsch.setKnownHosts("/home/foo/.ssh/known_hosts");
      
      String host=JOptionPane.showInputDialog("Enter username@hostname",
                                              System.getProperty("user.name")+
                                              "@localhost"); 
      String user=host.substring(0, host.indexOf('@'));
      host=host.substring(host.indexOf('@')+1);

      Session session=jsch.getSession(user, host, 22);
      //session.setPassword("your password");

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);

      //java.util.Hashtable config=new java.util.Hashtable();
      //config.put("StrictHostKeyChecking", "no");
      //session.setConfig(config);

      //session.connect();
      session.connect(3000);   // making a connection with timeout.

      Channel channel=session.openChannel("shell");

      channel.setInputStream(System.in);
      channel.setOutputStream(System.out);

      /*
      ((ChannelShell)channel).setPtyType("vt102");
      java.util.Hashtable env=new java.util.Hashtable();
      env.put("LANG", "ja_JP.eucJP");
      ((ChannelShell)channel).setEnv(env);
      */

      //channel.connect();
      channel.connect(3*1000);
    }
    catch(Exception e){
      System.out.println(e);
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
      else{ 
        return false; 
      }
    }
    public void showMessage(String message){
      JOptionPane.showMessageDialog(null, message);
    }
  }
}


