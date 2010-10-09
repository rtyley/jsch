/* -*-mode:java; c-basic-offset:2; -*- */
import com.jcraft.jsch.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

class KeyGen{
  public static void main(String[] arg){
    if(arg.length<2){
      System.err.println("usage: java KeyGen output_keyfile comment");
      System.exit(-1);
    }

    String filename=arg[0];
    String comment=arg[1];

    JSch jsch=new JSch();

    String passphrase="";
    JTextField passphraseField=(JTextField)new JPasswordField(20);
    Object[] ob={passphraseField};
    int result=
      JOptionPane.showConfirmDialog(null, ob, "Enter passphrase (empty for no passphrase)",
				    JOptionPane.OK_CANCEL_OPTION);
    if(result==JOptionPane.OK_OPTION){
      passphrase=passphraseField.getText();
    }

    try{
      KeyPair kpair=KeyPair.genKeyPair(jsch, KeyPair.DSA);
      kpair.setPassphrase(passphrase.getBytes());
      kpair.writePrivateKey(filename);
      kpair.writePublicKey(filename+".pub", comment);
      kpair.dispose();
    }
    catch(Exception e){
      System.out.println(e);
    }
    System.exit(0);
  }
}
