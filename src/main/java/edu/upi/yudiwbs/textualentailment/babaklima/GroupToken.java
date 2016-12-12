package edu.upi.yudiwbs.textualentailment.babaklima;

import java.util.ArrayList;

/**
 *
 * Created by yudiwbs on 09/04/2016.
 * digunakan untuk memproses tiruan umbc (lihat isi GloveUmbc)
 *
 */

public class GroupToken {
    ArrayList<String> tokenKata; //sisa, kata biasa
    ArrayList<String> tokenTgl;
    ArrayList<String> tokenUang;
    ArrayList<String> tokenAngka;
    ArrayList<String> tokenLokasi;

    //Prepro pp = new Prepro();
    Prepro pp;

    public GroupToken(Prepro pp) {
        this.pp = pp;
        //pp.loadStopWords("stopwords2","kata");
    }  //versi stopwords wash.edu plus tambahan

    //fs: arraylist para token terisi
    public void ambilToken(String kal,String ner) {
        //PROSES TOKEN DULU, nanti dipisah
        //kalau di paper asli dilematisasi, tapi setelah percobaan hasilnya tdk lebih baik (malah turun)
        //angka tidak dibuang


        Object[] objOut = pp.ambilTokenTgl(kal,ner);
        tokenTgl = (ArrayList<String>) objOut[0];
        String sisaKal = (String) objOut[1];
        //debug
        /*
        System.out.println("Ambil Tanggal:");
        for (String s:tokenTgl) {
            System.out.println(s);
        }
        */

        objOut = pp.ambilTokenUang(sisaKal,ner);
        tokenUang = (ArrayList<String>) objOut[0];
        sisaKal = (String) objOut[1];

        /*
        System.out.println("Ambil Uang:");
        for (String s:tokenUang) {
            System.out.println(s);
        }
        */
        //System.out.println("sisa="+sisaKal);

        //angka
        objOut = pp.ambilTokenAngka(sisaKal,ner);
        tokenAngka = (ArrayList<String>) objOut[0];
        sisaKal = (String) objOut[1];

        /*
        System.out.println("Ambil Angka:");
        for (String s:tokenAngka) {
            System.out.println(s);
        }
        */

        //lokasi
        /*
        objOut = pp.ambilTokenLokasi(sisaKal,ner);
        tokenLokasi = (ArrayList<String>) objOut[0];
        sisaKal = (String) objOut[1];
        System.out.println("Ambil Lokasi:");
        for (String s:tokenLokasi) {
            System.out.println(s);
        }
        */

        //utk person dan organiszation perlu?? --> nanti dicoba


        //TERAKHIR
        tokenKata  = pp.loadKataTanpaStopWords(sisaKal,true,true);
        /*
        System.out.println("Sisanya:");
        for (String s:tokenKata) {
            System.out.println(s);
        }
        */

    }
}
