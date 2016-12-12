package edu.upi.yudiwbs.textualentailment.babaklima;

import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by yudiwbs on 11/30/2015.
 *
 * menyipan objek T dan H, termasuk syntatic structure
 *
 * lihat prepro isiInfoTeks
 *
 */

public class InfoTeks {
    public int id; //idH atau idT
    public ArrayList<String>  alVerb = new ArrayList<>();    //dalam lowercase
    public ArrayList<String>  alNoun = new ArrayList<>();    //dalam lowercase
    public ArrayList<String>  alPronoun = new ArrayList<>(); //dalam lowercase
    public String strukturSyn; //struktur sintaks dari kalimat spt (ROOT (S (NP dst
    public String teksAsli;

    private class Param {
        ArrayList<String> tree;
        int pos;
        String tag;
    }

    /*
       berdasarkan teksAsli dan strukturSyn, isi ulang salVerb dan alNoun
     */
    public void isiArrListVerbNoun() {
        Prepro pp = new Prepro();
        InfoTeks it = pp.isiInfoTeks(teksAsli,strukturSyn);
        alVerb.clear();
        alNoun.clear();
        alVerb = it.alVerb;
        alNoun = it.alNoun;
        alPronoun = it.alPronoun;
    }

    /*
       internal dipanggil cariTag
       hasil sudah di trim
     */
    private  ArrayList<String> cariTagRekur(Param p) {
        ArrayList<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int kurung=0;
        boolean stop=false;
        while (!stop) {
            //mulai ambil isi
            p.pos++; //sudah pasti tag ketemu yg diawal
            kurung++;
            while (p.pos<p.tree.size()) {
                if (p.tree.get(p.pos).equals(")")) {
                    kurung--;
                    if (kurung==0) {
                        stop = true;
                        break; //selesai
                    }
                } else if (p.tree.get(p.pos).contains("("))  {
                    if (p.tree.get(p.pos).equals(p.tag)) {
                        //ketemu tag yg dicari di dalam
                        //rekursif
                        ArrayList<String> hasil = cariTagRekur(p);
                        out.addAll(hasil);
                        //add yang terkahir
                        sb.append(hasil.get(hasil.size()-1));
                        sb.append(" ");
                    } else {
                        kurung++;
                    }
                } else {
                    sb.append(p.tree.get(p.pos));
                    sb.append(" ");
                }
                p.pos++;
            }
            if (stop) {
                String hasil = sb.toString().trim();
                hasil = hasil.replace(" '","'"); //perbaiki spasi sebelum apostrof
                hasil = hasil.replace(" ,",","); //perbaiki spasi sebelum koma
                out.add(hasil);
            } else {
                sb.append("ERROR, KURUNG KURANG PASANGAN!!");
                stop = true;
            }
        }
        return out;
    }

    /*
       cari kelompok kata yang sesuai tag, support rekursif, contoh penggunaan lihat di method main
        Contoh:
        (ROOT (S (NP (NNP Police)) (VP (VBD recovered) (NP (CD 81) (NNP Andy) (NNP Warhol) (NNS lithographs))) (. .)))

        berddasarkan tag misal tag NNP hasilnya: {police, Andy, Warhol}

        tbd: perlu posisi kata juga sebenarnya

     */

    public ArrayList<String> cariTag(String  tag) {

        String tree = strukturSyn;

        String s;
        tag = "(" + tag;
        s = tree.replaceAll("\\)", " ) "); //biar kurung tutup tdk lengket

        //pindahkan string ke arraylist t
        Scanner sc = new Scanner(s);
        ArrayList<String> t = new ArrayList<>();
        while (sc.hasNext()) {
            t.add(sc.next());
        }

        Param p = new Param();
        p.tag = tag;
        p.tree = t;
        p.pos = 0;

        ArrayList<String> out = new ArrayList<>();
        int kurung=0;
        while (p.pos<p.tree.size()) {
            //System.out.println(p.tree.get(i));
            if (p.tree.get(p.pos).equals(p.tag)) {  //tag ketemu
                ArrayList<String> hasil = cariTagRekur(p);
                out.addAll(hasil);
            } else {
                p.pos++;
            }
        }
        return out;
    }

    public String getAllVerb() {
        StringBuilder sb = new StringBuilder();
        for (String v:alVerb) {
            sb.append(v);sb.append(" ");
        }
        return sb.toString().trim();
    }

    public String getAllNoun() {
        StringBuilder sb = new StringBuilder();
        for (String v:alNoun) {
            sb.append(v);sb.append(" ");
        }
        return sb.toString().trim();
    }

    public String getAllPronoun() {
        StringBuilder sb = new StringBuilder();
        for (String v:alPronoun) {
            sb.append(v);sb.append(" ");
        }
        return sb.toString().trim();
    }




    @Override
    public String toString() {

        System.out.println("Teks:"+teksAsli);
        StringBuilder sbTemp = new StringBuilder();
        sbTemp.append("Verb:");
        for (String s:alVerb) {
            //System.out.print(s);
            sbTemp.append(s);
            sbTemp.append(" ");
            //System.out.print(" ");
        }
        sbTemp.append(System.lineSeparator());

        sbTemp.append("Noun:");
        for (String s:alNoun) {
            sbTemp.append(s);
            sbTemp.append(" ");
        }
        sbTemp.append(System.lineSeparator());

        sbTemp.append("Pronoun:");
        for (String s:alNoun) {
            sbTemp.append(s);
            sbTemp.append(" ");
        }
        sbTemp.append(System.lineSeparator());

        return sbTemp.toString();
    }

    public static void main(String[] args) {
        //String s ="(ROOT (S (NP (NNP Amsterdam) (NNS police)) (VP (VBD said) (NP-TMP (NNP Wednesday)) (SBAR (IN that) (S (NP (NNP Amsterdam) (NNS police)) (VP (VBP have) (VP (VBN recovered) (NP (VBN stolen) (NNS lithographs)) (PP (IN by) (NP (NP (DT the) (JJ late) (NNP U.S.) (NN pop) (NN artist)) (NP (NNP Andy) (NNP Warhol)))) (PP (IN worth) (NP (QP (JJR more) (IN than) ($ $) (CD 1) (CD million))))))))) (. .)))";
        //String s   ="(ROOT (S  (VP (VBD said) (SBAR (IN that) (S (VP (VBP have) (VP (VBN recovered)  (PP (IN worth) (NP (QP (JJR more) (CD million))))))))) (. .)))";

        String s ="(ROOT (S (NP (NNP Mr) (NNP Fitzgerald)) (VP (VBD revealed) (SBAR (S (NP (NNP Mr) (NNP Fitzgerald))" +
                " (VP (VBD was) (NP (NP (CD one)) (PP (IN of) (NP (JJ several) (JJ top) (NNS officials)))" +
                " (SBAR (WHNP (WP who)) (S (VP (VBD told) (NP (NP (NNP Mr) (NNP Libby)) (PP (IN in)" +
                " (NP (NNP June) (CD 2003)))) (SBAR (IN that) (S (NP (NP (NNP Valerie) (NNP Plame))" +
                " (, ,) (NP (NP (NN wife)) (PP (IN of) (NP (DT the) (JJ former) (NN ambassador)" +
                " (NNP Joseph) (NNP Wilson)))) (, ,)) (VP (VBD worked) (PP (IN for) (NP (DT the) " +
                "(NNP CIA)))))))))))))) (. .)))";


        //String s ="(ROOT (S (S (NP (NP (NNP Genevieve) (IN de) (NNP Gaulle-Anthonics)) (, ,) (NP (CD 81)) (, ,)) (VP (VBP niece) (PP (IN of) (NP (NP (DT the) (JJ late) (NNP Charles) (FW de) (NNP Gaulle)) (, ,) (VP (VBN died) (PP (IN in) (NP (NNP Paris))) (PP (IN on) (NP (NNP February) (CD 14) (, ,) (CD 2002))) (. .)))))) (NP (PRP She)) (VP (VBD joined) (NP (DT the) (JJ French) (NN resistance)) (SBAR (WHADVP (WRB when)) (S (NP (DT the) (NNPS Germans)) (VP (VBD occupied) (NP (NNP Paris)))))) (. .)))";

        InfoTeks it= new InfoTeks();
        it.strukturSyn = s;
        ArrayList<String> hasil =  it.cariTag(",");
        for (String tempS:hasil) {
            System.out.println(tempS);
        }

    }

}
