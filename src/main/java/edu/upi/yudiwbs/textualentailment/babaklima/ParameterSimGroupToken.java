package edu.upi.yudiwbs.textualentailment.babaklima;

/**
 * Created by yudiwbs on 07/08/2016.
 * default paramater adalah versi yg terbaik (sebelum optimasi)
 *
 */

public class ParameterSimGroupToken {
    double  penaltiAngka = 1;  //umbc: 1
    double  penaltiLokasi = 0.5; //umbc: tdk ada
    double  penaltiTgl = 0.5;  //umbc: 0.5
    double  penaltiUang = 0.5; //umbc: 0.5

    double  penaltiKataVerbNoun = 1;  //umbc 1
    double  penaltiKataLain = 0.5;    //umbc 0.5
    double  batasPenaltiKata = 0.25;
    double  penaltiKalNeg = 1;

    @Override
    public String toString() {
        return "penaltiAngka:"+penaltiAngka+" penaltiLokasi:"
                +penaltiLokasi+" penaltiTgl:"+penaltiTgl+" penaltiUang:"+penaltiUang
                +" penaltiKataVerbNoun:"+penaltiKataVerbNoun+" penaltiKataLain:"+penaltiKataLain
                +" penaltiKalNeg:"+penaltiKalNeg+ " batasPenaltiKata:"+batasPenaltiKata;
    }
}
