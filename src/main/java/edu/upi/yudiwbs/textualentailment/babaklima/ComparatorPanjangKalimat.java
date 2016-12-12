package edu.upi.yudiwbs.textualentailment.babaklima;

import java.util.ArrayList;

/**
 * Created by yudi
 *
 *   untuk sort kalimat berdasarkan panjang kalimat
 */
public class ComparatorPanjangKalimat implements java.util.Comparator<String> {
    boolean isPendekKePanjang = true;

    public ComparatorPanjangKalimat(boolean vIsPendekKePanjang) {
        isPendekKePanjang = vIsPendekKePanjang;
    }

    public int compare(String s1, String s2) {
        if (isPendekKePanjang) {
            return s1.length() - s2.length();
        } else  {
            return s2.length() - s1.length();
        }
    }

    public static void main(String[] args) {
        //test
        ArrayList<String> t = new ArrayList<>();
        t.add("satu");
        t.add("palingpalingpanjang");
        t.add("abc");
        t.add("satudua");
        t.add("panjangpanjang");

        ComparatorPanjangKalimat comparator = new ComparatorPanjangKalimat(false);
        java.util.Collections.sort(t, comparator);
        for (String s: t) {
            System.out.println(s);
        }


    }
}