package edu.upi.yudiwbs.textualentailment.babaklima;

import weka.classifiers.trees.j48.C45Split;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * IsiWordEmbedUmbc
 *
 *   versi baru yang lebih otomatis, versi lama lihat
 *   di TextualEntailment edu.upi.... babak4
 *
 *
 *   untuk model
 *
 *   - lihat di bagian init untuk setvectornya
 *   - untuk set penalti lihat class simGroupToken
 *     adaptasi dari paper UMBC (han2013)
 *
 *   lihat isiWordUmbcVer3 untuk versi yang mengotomatisiasi parameter
 *
 */

public class IsiWordEmbedUmbc {
    public String namaTabel;
    public ModelVector modelVector;
    public SimGroupToken sgt;

    private String fileArff = "D:\\desertasi\\final\\eksperimen\\out.arff";  //dummy, bisa ditaro di dir temp
    private Connection conn = null;
    private PreparedStatement pSel = null;


    ResultSet rs = null;
    Prepro pp;


    //skor disimpan langsung ke file arff
    //nama tabel menyimpan data training
    public void init(String vnamaTabel) {
        namaTabel = vnamaTabel;
        KoneksiDB db = new KoneksiDB();
        pp = new Prepro();
        pp.loadStopWords("stopwords","kata");
        //skor_word2vec_verb,skor_word2vec_noun,skor_word2vec_verbHnounT
        try {
            conn = db.getConn();
            //jika sudah ada isi lagi (dikomentari yg bagian is null)
            String strSel = "select id,t,h, t_gram_structure, " +
                    "h_gram_structure,t_ner, h_ner, isEntail " +
                    " from " + namaTabel + " #limit 10" ;


            //String strUpdate = "update "+namaTabel+ " set "+kolomTujuan+"=? " +
            //        " where id=? ";

            pSel = conn.prepareStatement(strSel);
            //pUpd = conn.prepareStatement(strUpdate);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //paling akhir, karena lama

        //ini yang penting
        //paragram sl999: lebih bagus pada hasil testing
        ParameterSimGroupToken param = new ParameterSimGroupToken();
        Prepro pp = new Prepro();
        pp.loadStopWords("stopwords2","kata");
        //sgt = new SimGroupToken(0,"D:\\eksperimen\\paragram\\paragram_300_sl999\\paragram_300_sl999\\paragram_300_sl999.txt",param,pp);
        sgt = new SimGroupToken(modelVector,param,pp);

        //glove
        /*
        sgt = new SimGroupToken("D:\\eksperimen\\glove\\glove.6B.300d.txt",
                "D:\\eksperimen\\textualentailment\\GoogleNews-vectors-negative300.bin.gz");
        */


        //sgt = new SimGroupToken("D:\\eksperimen\\textualentailment\\GoogleNews-vectors-negative300.bin.gz");
        //System.out.println("Menggunakan W2Vec");

        //batas terendah sebelum kena penalti
    }

    @Override
    public void finalize() {
        close();
    }

    public void close() {
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

    /*
         hitung skor data training untuk mendapatkan thredshold
         loop setiap pasang T-H di tabel
           hitung skor jarak
           modifikasi: tidak tulis ke tabel tapi tulis ke file format arff

           @relation 'multiparam'

           @attribute is_Entail {TRUE,FALSE}
           @attribute data numeric

           @data
           TRUE,1,1,1,1,1,1,1
     */
    public void proses() {
        rs = null;
        try {
            rs = pSel.executeQuery();
            PrintWriter pw = new PrintWriter(fileArff);
            pw.println("@relation 'multiparam'");
            pw.println("@attribute is_Entail {true,false}");
            pw.println("@attribute data numeric");
            pw.println("@data");
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

                //debug print
                //System.out.println("");
                //System.out.println("id:"+id);
                //System.out.println("T:"+t);
                //System.out.println("H:"+h);
                //skor_word2vec_verb,skor_word2vec_noun,skor_word2vec_verbHnounT
                //update skor
                //double jarak = jarakMaks(t,h,tNer,hNer);

                /*
                System.out.println("T:"+t);
                System.out.println("H:"+h);
                System.out.println("IsEntail:----------->" +
                        ""+isEntail);
                */

                //isi group token
                //tNer dan hNer diambil dari database (sudah diproses sebelumnya)
                GroupToken gtT = new GroupToken(pp);
                gtT.ambilToken(t,tNer);
                GroupToken gtH = new GroupToken(pp);
                gtH.ambilToken(h,hNer);

                //nanti bisa gabung pengisian variabelnya
                sgt.setGroupToken(gtT,gtH);
                sgt.setTH(t,h);
                sgt.setPosTag(tSynTree,hSynTree);
                double jarak = sgt.getSim();
                //System.out.println("Jarak:"+jarak);

                /*
                pUpd.setDouble(1, jarak);
                pUpd.setLong(2, id);
                pUpd.executeUpdate();
                */
                //TRUE,1,1,1,1,1,1,1
                pw.println(isEntail+","+jarak);
            }
            pw.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public double hitungThreshold() {
        double t = 0;
        Instances ins = null;
        try {
            ins = ConverterUtils.DataSource.read(fileArff);
            //if (ins.classIndex() == -1)
            //    ins.setClassIndex(result.numAttributes() - 1);
            ins.setClassIndex(0);
            C45Split split=new C45Split(1, 0, ins.sumOfWeights(), true);
            split.buildClassifier(ins);
            t = split.splitPoint();
            System.out.println("threshold:"+t);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return t;
    }

    //ambil data t,h dari tabel, berdasarkan threshold an parameter lalu menghitung akurasi
    public double klasifikasi(double threshold, String namaTabelTest) {
        double akurasiTerakhir=0;
        PreparedStatement pSelTest = null;
        String strSelTest = "select id,t,h, t_gram_structure, " +
                "h_gram_structure,t_ner, h_ner, isEntail " +
                " from " + namaTabelTest + " #limit 10" ; //

        rs = null;
        try {
            pSelTest = conn.prepareStatement(strSelTest);
            rs = pSelTest.executeQuery();
            int jumPredCocok = 0;
            int cc = 0;
            StringBuilder sb = new StringBuilder();
            sb.append("id,isCocok");
            sb.append(System.getProperty("line.separator"));

            while (rs.next()) {
                //id,t,h, t_gram_structure, h_gram_structure
                cc++;
                int id = rs.getInt(1);
                String t = rs.getString(2);
                String h = rs.getString(3);
                String tSynTree = rs.getString(4);
                String hSynTree = rs.getString(5);
                String tNer = rs.getString(6);
                String hNer = rs.getString(7);
                Boolean isEntail = rs.getBoolean(8);

                //debug print
                //System.out.println("");
                //System.out.println("id:"+id);
                //System.out.println("T:"+t);
                //System.out.println("H:"+h);
                //skor_word2vec_verb,skor_word2vec_noun,skor_word2vec_verbHnounT
                //update skor
                //double jarak = jarakMaks(t,h,tNer,hNer);
                /*
                System.out.println("T:"+t);
                System.out.println("H:"+h);
                System.out.println("IsEntail:----------->" +
                        ""+isEntail);
                */
                //isi group token
                GroupToken gtT = new GroupToken(pp);
                gtT.ambilToken(t,tNer);
                GroupToken gtH = new GroupToken(pp);
                gtH.ambilToken(h,hNer);

                //nanti bisa gabung pengisian variabelnya
                sgt.setGroupToken(gtT,gtH);
                sgt.setTH(t,h);
                sgt.setPosTag(tSynTree,hSynTree);

                double jarak = sgt.getSim();


                boolean predEntail;
                predEntail = (jarak>threshold);

                /*
                System.out.println("Jarak:"+jarak);
                System.out.println("PredEntail:"+predEntail);
                */
                if (isEntail == predEntail) {
                    jumPredCocok++;
                    sb.append(id+","+1);
                } else {
                    sb.append(id+","+0);
                }
                sb.append(System.getProperty("line.separator"));
            }
            System.out.println("Jumlah pred cocok:"+jumPredCocok);
            akurasiTerakhir = (double) jumPredCocok/cc;
            //System.out.println("Akurasi:"+akurasiTerakhir);
            //System.out.println("Debug Rincian");
            //System.out.println(sb.toString());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return akurasiTerakhir;
    }


    public static void main(String[] args) {
        //yg 840B token saat dijalankan makan waktu 3 jam dan akhirnya kehabisan memori, test lagi nanti
        //840B tokens, 2.2M vocab, cased, 300d vectors, 2.03 GB download
        //String str840 = "D:\\eksperimen\\glove\\glove.840B.300d\\glove.840B.300d.txt";
        //6B tokens, 400K vocab, uncased, 50d, 100d, 200d, & 300d
        //String str6 ="D:\\eksperimen\\glove\\glove.6B.300d.txt";
        //42B tokens, 1.9M vocab, uncased, 300d vectors, hasil kurang
        //String str42 = "D:\\eksperimen\\glove\\glove.42B.300d\\glove.42B.300d.txt";

        IsiWordEmbedUmbc iw = new IsiWordEmbedUmbc();
        ModelVectorW2VecCustom W2VecCustom = new ModelVectorW2VecCustom();
        W2VecCustom.loadModel("D:\\desertasi\\final\\eksperimen\\model\\word2vec_wikien_minword5_layer100_windowsize5_cbow.txt");
        iw.modelVector = W2VecCustom;
        iw.init("rte3_babak2");
        iw.proses();
        double threshold = iw.hitungThreshold();
        double akurasi = iw.klasifikasi(threshold,"rte3_test_gold");
        System.out.println("Akurasi:"+akurasi);
        iw.close();
    }
}
