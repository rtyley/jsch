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
      JSch jsch=new JSch();
      String host=JOptionPane.showInputDialog("Please enter hostname", 
					      "localhost"); 
      Session session=jsch.getSession(host, 22);

      String foo=JOptionPane.showInputDialog("Please enter -L", 
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
