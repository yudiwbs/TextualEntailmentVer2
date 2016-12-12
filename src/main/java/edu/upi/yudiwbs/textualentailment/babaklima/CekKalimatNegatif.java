package edu.upi.yudiwbs.textualentailment.babaklima;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * lihat ProsesKalimatNegatif
 * kenapa diberlabel old?
 *
 * Created by yudiwbs on 25/04/2016.
 * mengecek apakah suatu kalimat negatif
 *
 *
 *  CVS will stop selling its own brand of 500-milligram acetaminophen caplets
 *  and pull bottles from store shelves nation wide, spokesman Mike DeAngelis said.
 *
 *



 (ROOT (S (NP (NNP CVS)) (VP (MD will) (RB not) (VP (VB sell) (NP (NP (PRP$ its)
 (JJ own) (NN brand)) (PP (IN of) (NP (JJ 500-milligram) (NN acetaminophen)
 (NNS caplets)))) (ADVP (DT any) (RBR longer)))) (. .)))


 ada however, membatalkan kalimat negatif

 T:The British government did not initially purchase the weapon and civilian
 sales were modest. However the U.S. Civil War began in 1860 and the governments
 of both the United States and the Confederacy began purchasing arms in Britain.
 H:During the Civil War the government of the United States bought arms from Britain.



 kesesuain verb (get sick vs spread)

 T:Female mosquitoes become infected with the malaria parasite when they draw blood from humans with malaria. The
 insects can then pass this on to other humans they bite, but do not get sick themselves.

 H:Female mosquitoes spread malaria.

 kesesuaian verb (active combat != part of     )
 T:As an active member of the National Guard, he was called to duty in 1941.
 Although Kennon did not see active combat, he did not return home from World War II
 until May of 1945.
 H:Kennon was part of the National Guard.
 IsEntail:----------->true


 beda verb

 id:146
 T:So far Sony BMG has not released a list of how many of its CDs are protected or how many have been sold.
 H:Sony BMG sells protected CDs.
 IsEntail:----------->true
 T negatif




 *
 */

public class CekKalimatNegatif {

    Prepro pp;

    public CekKalimatNegatif(Prepro pp) {
        this.pp = pp;
        //pp.loadStopWords("stopwords2","kata");
    }

    public StructCariKalNegatif isKalimatNegatif(InfoTeks it) {
        StructCariKalNegatif  out = new StructCariKalNegatif();
        out.verb="";
        Boolean isAdaNeg = false;
        //pp.
        ArrayList<String> alRB = it.cariTag("RB");

        //cari kata not
        for (String s:alRB) {
            if (s.equals("not")) {
                isAdaNeg = true;
                //cari posisi
                break;
            }
        }

        if (isAdaNeg) {
           Scanner sc = new Scanner(it.teksAsli);
           boolean ketemuNot=false;
           while (sc.hasNext()) {
                String s = sc.next();
                if (s.equals("not")) {
                   ketemuNot = true;
                   break;
                }
           }
           if (ketemuNot) {  //harusnya pasti ketemu
               //ambil verb setelah not
               StringBuilder sbVerb = new StringBuilder();
               boolean adaVerb=false;
               while (sc.hasNext()) {
                   String s = sc.next();
                   if (it.alVerb.contains(s)) {
                      adaVerb = true;
                      sbVerb.append(s);
                      sbVerb.append(" ");
                   } else {
                       //stop jika sudah ada verb yg masuk
                       if (adaVerb) {
                           break;
                       }
                   }
               }
               String v = pp.loadKataTanpaStopWordstoString(sbVerb.toString().trim(),true,true);
               out.verb = v;
           }

        }

        out.isNegatif = isAdaNeg;
        return out;
    }

    public static void main(String[] args) {
        KoneksiDB db = new KoneksiDB();
        Connection conn = null;
        PreparedStatement pSel = null;
        String namaTabel = "rte3_babak2";
        ResultSet rs=null;
        Prepro pp = new Prepro();
       //debug
        try {
            conn = db.getConn();
            //jika sudah ada isi lagi (dikomentari yg bagian is null)
            String strSel = "select id,t,h, t_gram_structure, " +
                    "h_gram_structure,t_ner, h_ner, isEntail " +
                    " from " + namaTabel + " #limit 10" ; //ditabatasi dulu sepuluh

            pSel = conn.prepareStatement(strSel);

            rs = pSel.executeQuery();
            CekKalimatNegatif ck = new CekKalimatNegatif(pp);
            while (rs.next()) {
                //id,t,h, t_gram_structure, h_gram_structure
                int id = rs.getInt(1);
                String t = rs.getString(2);
                String h = rs.getString(3);
                String tSynTree = rs.getString(4);
                String hSynTree = rs.getString(5);
                String tNer = rs.getString(6);
                String hNer = rs.getString(7);
                Boolean isEntail = rs.getBoolean(8);

                InfoTeks itT = pp.isiInfoTeks(t,tSynTree);
                InfoTeks itH = pp.isiInfoTeks(h,hSynTree);

                StructCariKalNegatif tNeg = ck.isKalimatNegatif(itT);
                StructCariKalNegatif hNeg = ck.isKalimatNegatif(itH);

                if (tNeg.isNegatif||hNeg.isNegatif) {
                    if (tNeg.isNegatif) {
                        System.out.println("T negatif");
                        System.out.println("verb:"+tNeg.verb);
                    }
                    if (hNeg.isNegatif) {
                        System.out.println("H negatif");
                        System.out.println("verb:"+hNeg.verb);
                    }

                    //debug print
                    System.out.println("");
                    System.out.println("id:"+id);
                    System.out.println("T:"+t);
                    System.out.println("H:"+h);
                    System.out.println("IsEntail:----------->" +
                            ""+isEntail);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            pSel.close();
            rs.close();
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
