import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Sftp{
  public static void main(String[] arg){

    try{
      String host=null;
      if(arg.length==1){ host=arg[0]; }
      else{ host=JOptionPane.showInputDialog("Enter hostname", "localhost"); }

      JSch jsch=new JSch();
      Session session=jsch.getSession(host, 22);

      // username and password will be given via UserInfo interface.
      UserInfo ui=new MyUserInfo();
      session.setUserInfo(ui);

      //java.util.Properties config=new java.util.Properties();
      //config.put("compression.s2c", "zlib");
      //config.put("compression.c2s", "zlib");
      //session.setConfig(config);

      session.connect();

      Channel channel=session.openChannel("sftp");
      channel.connect();
      ChannelSftp c=(ChannelSftp)channel;

      java.io.InputStream in=System.in;
      java.io.PrintStream out=System.out;

      java.util.Vector cmds=new java.util.Vector();
      byte[] buf=new byte[1024];
      int i;
      String str;

      while(true){
        out.print("sftp> ");
	cmds.removeAllElements();
        i=in.read(buf, 0, 1024);
	if(i<=0)break;

        i--;
        if(i>0 && buf[i-1]==0x0d)i--;
        //str=new String(buf, 0, i);
        //System.out.println("|"+str+"|");
	int s=0;
	for(int ii=0; ii<i; ii++){
          if(buf[ii]==' '){
            if(ii-s>0){ cmds.addElement(new String(buf, s, ii-s)); }
	    while(ii<i){if(buf[ii]!=' ')break; ii++;}
	    s=ii;
	  }
	}
	if(s<i){ cmds.addElement(new String(buf, s, i-s)); }
	if(cmds.size()==0)continue;

	String cmd=(String)cmds.elementAt(0);
	if(cmd.equals("quit")){
          c.quit();
	  break;
	}
	if(cmd.equals("exit")){
          c.exit();
	  break;
	}
	if(cmd.equals("cd") || cmd.equals("lcd")){
          if(cmds.size()<2) continue;
	  String path=(String)cmds.elementAt(1);
          if(cmd.equals("cd")) c.cd(path);
	  else c.lcd(path);
	  continue;
	}
	if(cmd.equals("rm") || cmd.equals("rmdir")){
          if(cmds.size()<2) continue;
	  String path=(String)cmds.elementAt(1);
          if(cmd.equals("rm")) c.rm(path);
	  else c.rmdir(path);
	  continue;
	}
	if(cmd.equals("pwd") || cmd.equals("lpwd")){
           str=(cmd.equals("pwd")?"Remote":"Local");
	   str+=" working directory: ";
          if(cmd.equals("pwd")) str+=c.pwd();
	  else str+=c.lpwd();
	  out.println(str);
	  continue;
	}
	if(cmd.equals("ls") || cmd.equals("dir")){
	  String path=".";
	  if(cmds.size()==2) path=(String)cmds.elementAt(1);
	  java.util.Vector vv=c.ls(path);
	  if(vv!=null){
	    for(int ii=0; ii<vv.size(); ii++){
              out.println(vv.elementAt(ii));
	    }
	  }
	  continue;
	}
	if(cmd.equals("get") || cmd.equals("put")){
	  if(cmds.size()!=2 && cmds.size()!=3) continue;
	  String p1=(String)cmds.elementAt(1);
	  String p2=p1;
	  if(cmds.size()==3)p2=(String)cmds.elementAt(2);
	  if(cmd.equals("get")) c.get(p1, p2);
	  else  c.put(p1, p2);
	  continue;
	}
	if(cmd.equals("ln") || cmd.equals("symlink") || cmd.equals("rename")){
          if(cmds.size()!=3) continue;
	  String p1=(String)cmds.elementAt(1);
	  String p2=(String)cmds.elementAt(2);
	  if(cmd.equals("rename")) c.rename(p1, p2);
	  else c.symlink(p1, p2);
	  continue;
	}
	if(cmd.equals("version")){
	  out.println("SFTP protocol version "+c.version());
	  continue;
	}
	if(cmd.equals("help") || cmd.equals("help")){
	  out.println(help);
	  continue;
	}
        out.println("unimplemented command: "+cmd);
      }
    }
    catch(Exception e){
      System.out.println(e);
    }
    System.exit(0);
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

  private static String help =
"      Available commands:\n"+
"      * means unimplemented command.\n"+
"cd path                       Change remote directory to 'path'\n"+
"lcd path                      Change local directory to 'path'\n"+
"*chgrp grp path               Change group of file 'path' to 'grp'\n"+
"*chmod mode path              Change permissions of file 'path' to 'mode'\n"+
"*chown own path               Change owner of file 'path' to 'own'\n"+
"help                          Display this help text\n"+
"get remote-path [local-path]  Download file\n"+
"*lls [ls-options [path]]      Display local directory listing\n"+
"ln oldpath newpath            Symlink remote file\n"+
"*lmkdir path                  Create local directory\n"+
"lpwd                          Print local working directory\n"+
"ls [path]                     Display remote directory listing\n"+
"*lumask umask                 Set local umask to 'umask'\n"+
"mkdir path                    Create remote directory\n"+
"put local-path [remote-path]  Upload file\n"+
"pwd                           Display remote working directory\n"+
"exit                          Quit sftp\n"+
"quit                          Quit sftp\n"+
"rename oldpath newpath        Rename remote file\n"+
"rmdir path                    Remove remote directory\n"+
"rm path                       Delete remote file\n"+
"symlink oldpath newpath       Symlink remote file\n"+
"version                       Show SFTP version\n"+
"?                             Synonym for help";
}
