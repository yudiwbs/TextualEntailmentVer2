package edu.upi.yudiwbs.textualentailment.babaklima;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by Yudi on 11/26/2014.
 *
 *  memusatkan koneksi ke DB
 *
 *
 */
public class KoneksiDB {
    public String propFileName="resources/conf/db.properties";  //default, tapi bisa diganti
    String koneksiDB = "";
    String userPwd="";


    private void getRes() throws IOException {

        Properties prop = new Properties();
        InputStream input = null;
        //String propFileName = "resources/conf/db.properties";
        //diletakkan di tempat .class
        //String propFileName = "db.properties";
        //InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
        input = new FileInputStream(propFileName);
        if (input != null) {
            prop.load(input);
        } else {
            throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
        }

        // get the property value and print it out
        String passwd = prop.getProperty("passwd");
        String user = prop.getProperty("user");
        String db   = prop.getProperty("database");
        String host = prop.getProperty("host");

        //jadikan string
        //String koneksiDb = "jdbc:mysql://localhost/textualentailment?";
        //String userPwd   = "user=textentailment&password=textentailment";
        koneksiDB = "jdbc:mysql://"+host+'/'+db+'?';
        userPwd = "user="+user+'&'+"password="+passwd;


    }

    public  Connection getConn() throws SQLException, IOException, ClassNotFoundException {
        getRes();
        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection(koneksiDB+userPwd);
    }

    public static void main(String [] args) {
        KoneksiDB db = new KoneksiDB();
        try {
            //testing
            db.getRes();
            System.out.println(db.koneksiDB);
            System.out.println(db.userPwd);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
