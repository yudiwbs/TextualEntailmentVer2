package edu.upi.yudiwbs.textualentailment.babaklima;



import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yudiwbs on 09/04/2016.
 *  menghitung jarak/similarity antar dua grup token
 *
 *
 * Dipanggil oleh IsiWordEmbedUmbc
 *
 *   fix1: perbaikan proses tgl, angka, buang dash
 *   fix3: penalti untuk kesamaan lokasi
 *
 *   agt 16: tambah parameter
 *
 *   berantakan banget saat menggunakan glove atau word2vec, perlu diperbaiki serius
 *
 */

public class SimGroupToken {
    private ParameterSimGroupToken parameter;
    private ModelVector modelVec;


    //WordVectors vec;
    //Word2Vec vecWord2Vec = null;
    //HATI-HATI, NANTI BALIKIN!!!

    //Word2Vec  vecGlove  = null;  //hack!! hati2 kalau pake glove benerean
    //WordVectors vecW2V  = null;
    //int isGloveOrW2vec;

    GroupToken gtTT=null;
    GroupToken gtHH=null;

    //HATI2
    //nggak bisa pake remove stopwords karena perlu memproses dengan teks original
    Prepro pp;


    Prepro ppStopWords;   //khusus utk proses stopwords

    String posTagT;InfoTeks itT;
    String posTagH;InfoTeks itH;
    String t,h;
    //ProsesLemma pLemma;
    //ProsesWordnet pWordnet;

    //Options optTime;

    /*
    fix2_ver2: bobot penalti kata dikurangi 0.4 menjadi 0.6 utk verbnoun dan 0.1 untuk
    kata lainnya.
     */


    //final static  double  penaltiSubjTdkCocok = 1;   //coba dgn 2 dan 3 hasilnya buruk

    public void setParam(ParameterSimGroupToken par) {
        this.parameter = par;
    }



    //inisialisasinya bisa digabung nanti
    public void setTH(String t, String h) {
        this.t = t;
        this.h = h;
    }

    public void setGroupToken(GroupToken gtT,GroupToken gtH) {
        this.gtTT = gtT;
        this.gtHH = gtH;
    }

    //FS: itH dan itT terisi
    public void setPosTag(String posTagT, String posTagH) {
        this.posTagT = posTagT;
        this.posTagH = posTagH;

        itT = pp.isiInfoTeks(t,posTagT);
        itH = pp.isiInfoTeks(h,posTagH);
    }

    //output di trim dan di lowercase
    //fix: output dibuang stopwordsnya
    private String getSubj(InfoTeks it, String vKataPred) {
        String out = "";


        ArrayList<String> alNP = it.cariTag("NP");
        int posPred = it.teksAsli.indexOf(vKataPred);

        //cari NP yang terdekat tapi tidak melewati
        String lastNP="";
        for (String np:alNP) {
            int posNP = it.teksAsli.indexOf(np);
            if (posNP>posPred) break;//selesai
            if (posNP<0) continue; //harusnya sih pasti ketemu
            int posAkhir = posNP+np.length();
            if (posAkhir<posPred) {
                lastNP = np;
            }
        }

        out = lastNP;


        ArrayList<String> alKata = ppStopWords.loadKataTanpaStopWords(out,true,true);

        StringBuilder sb = new StringBuilder();
        for (String s:alKata) {
            sb.append(s);
            sb.append(" ");
        }
        out = sb.toString().trim();

        return out;
    }

    private boolean cekSubjCocok(String predH, String predT) {
        /* mengambil subject berbentuk NP (diasumsikan subject)
         yang terkait dengan  predikat T dan predikat H
        contoh:
        id:113
        T:Unfortunately, a visit from Mrs Hobday, causes Mr Browne to leave for London.
        H:Mrs Hobday departs London.

        predH: departs  -> subjeck: Mrs Hobday
        predT: leave    --> subject Mr. Browne

        tidak cocok, return false (utk nanti kena penalti)

        */

        Boolean out = true;
        //cari nounphrase yang terdekat dengan predH dan predT?
        //itH.alNoun

        String subjH = getSubj(itH,predH).toLowerCase();
        String subjT = getSubj(itT,predT).toLowerCase();

        /*
        System.out.println("Subyek H ("+predH+")="+subjH);
        System.out.println("Subyek T ("+predT+")="+subjT);
        */
        //true jika salah satu contain

        if (subjH.equals("")||subjT.equals("")) {
            out = subjH.equals("")&&subjT.equals("");
        } else {
            out = ((subjH.contains(subjT)) || (subjT.contains(subjH)));
        }
        return out;
    }




    //public SimGroupToken(String fileVecGlove,String fileVecW2V) {
    //parameter dipisah
    //public SimGroupToken(int vIsGloveOrW2vec,String fileVec, ParameterSimGroupToken param) {
    public SimGroupToken(ModelVector mv, ParameterSimGroupToken par, Prepro ppStopWords) {

        this.ppStopWords = ppStopWords;  //biar hanya sekali stopwordsnya diload

        this.pp = new Prepro();

        //parameter = param;
        setParam(par);

        modelVec = mv;  //model harus sudah diload!!
        //isGloveOrW2vec = vIsGloveOrW2vec;
        //isGloveOrW2vec, 0: glove, 1: w2vec

        //pLemma   = new ProsesLemma(); //panggil sekali, karena ada proses load
        //tidak perlu wordnet
        //pWordnet = new ProsesWordnet();


        //optTime = new Options();
        //optTime.setCompatibilityMode(true); //untuk apa??
        //opt.setGuess(true); //untuk apa??

        //load vector GLOVE <--- bukan, tapi vektor teks
        //if (isGloveOrW2vec==0) {
            //try {
         //       System.out.println("Mulai Load model teks. Mungkin agak lama ...");
                //vecW2V = WordVectorSerializer.loadGoogleModel(new File(fileVecW2V), true);
                //vecGlove = WordVectorSerializer.loadTxtVectors(new File(fileVec));
                //vecWord2Vec = WordVectorSerializer.r
                //vecGlove = WordVectorSerializer.readWord2Vec()
                //        readWord2(new File(fileVec));
                //HATI-HATI!!! UNTUK EKSPERIMEN WORD2VEC INI NAMANYA AJA veGLOVE
                //TAPI ISISNYA WORD2VEC,
                //todo: DIPERBAIKI NANTI
                //vecGlove = WordVectorSerializer.readWord2Vec(new File(fileVec));
                //System.out.println("Load selesai... ");
            //} catch (Exception ex) {
            //    ex.printStackTrace();
            //    System.exit(-1);
            //}
        //}

       //else
        //if (isGloveOrW2vec==1) {
            //load
            //load vector W2V
      //      try {
     //          System.out.println("Mulai Load model w2vec zip, agak lama ...");
      //          vecW2V = WordVectorSerializer.loadGoogleModel(new File(fileVec), true);
    //           System.out.println("Load selesai... ");
      //      } catch (Exception ex) {
     //           ex.printStackTrace();
     //           System.exit(-1);
     //       }
     //   }

    }

    //periksa dari gH -> gT
    //dipisah supaya nanti bisa dibalik (dua arah spt UMBC)
    //walaupun yg dua arah hasilnya jelek
    private double getSimInternal(GroupToken gtT, GroupToken gtH) {
        double out = 0;
        assert (gtH!=null && gtT!=null);
        double totalSkor = 0;  //ditambah
        //double totalSkorKali = 1; //dikali
        //hitung penalti bagi yg skornya rendah
        double totalPenalti  = 0;

        //double totalPenaltiKali = 1; //dikali, cuma aneh karena pasti kena penalti semua

        //penalti *belum* memperhitunkan bobot 1/log (freq) spt di paper


        //cari mulai dari H

        //lokasi
        //tambah lokasi malah jadi turun ya
        //kalau dihilangkan jangan lupa update perhitungan jumlah token dibawah
        //lalu lihat juga di class GroupToken

        /*
        if (gtH.tokenLokasi.size()>0) {
            for (String sH: gtH.tokenLokasi) {
                System.out.println("Cari lokasi:"+sH);
                boolean ketemuCocok = false;
                for (String sT: gtT.tokenLokasi) {
                    if (sH.equals(sT)) {
                        ketemuCocok = true;
                        totalSkor++;
                        //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                        System.out.println(sH+"->"+sT);
                        break; //proses sH berikutnya
                    }
                }
                if (!ketemuCocok) { //kena penalti
                    double penalti = penaltiLokasi;
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali * penalti;
                    System.out.println("Tdk ada pasangan, kena penalti:"+sH+"("+penalti+")");
                }
            }
        }
        */

        //uang
        if (gtH.tokenUang.size()>0) {
            for (String sH: gtH.tokenUang) {
                //System.out.println("Cari uang:"+sH);
                boolean ketemuCocok = false;
                for (String sT: gtT.tokenUang) {
                    if (sH.equals(sT)) {
                        ketemuCocok = true;
                        totalSkor++;
                        //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                        //System.out.println(sH+"->"+sT);
                        break; //proses sH berikutnya
                    }
                }
                if (!ketemuCocok) { //kena penalti
                    double penalti = parameter.penaltiUang;
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali * penalti;
                    //System.out.println("Tdk ada pasangan, kena penalti:"+sH+"("+penalti+")");
                }
            }
        }

        //angka
        if (gtH.tokenAngka.size()>0) {
            for (String sH: gtH.tokenAngka) {
                //System.out.println("Cari angka:"+sH);
                boolean ketemuCocok = false;
                for (String sT: gtT.tokenAngka) {
                    if (sH.equals(sT)) {
                        ketemuCocok = true;
                        totalSkor++;;
                        //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                        //System.out.println(sH+"->"+sT);
                        break; //proses sH berikutnya
                    }
                }
                if (!ketemuCocok) { //kena penalti
                    double penalti = parameter.penaltiAngka; //angka penaltinya besar
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali * penalti;
                    //System.out.println("Tdk ada pasangan, kena penalti:"+sH+"("+penalti+")");
                }
            }
        }

        /*

         @Override
    public void init() {

    }

         */

        //tanggal
        String pattern = "(15|16|17|18|19|20)\\d{2}";
        Pattern pTahun;
        pTahun = Pattern.compile(pattern);
        if (gtH.tokenTgl.size()>0) {
            for (String sH: gtH.tokenTgl) {
                boolean ketemuCocok = false;
                //System.out.println("Cari tgl:"+sH);

                //cocokan  tahun
                Matcher mH = pTahun.matcher(sH);
                ArrayList<String> alTahunH = new ArrayList<>();
                while (mH.find()) {
                    alTahunH.add(mH.group());
                    //System.out.println("tahunH:"+mH.group());
                }
                String strTCocok="";
                for (String sT : gtT.tokenTgl) {
                    //cari tahun
                    Matcher mT = pTahun.matcher(sT);
                    ArrayList<String> alTahunT = new ArrayList<>();
                    while (mT.find()) {
                        alTahunT.add(mT.group());
                       // System.out.println("tahunT:"+mT.group());
                    }

                    if (alTahunH.size()>0) {
                        //ada tahun di H
                        for (String tahunH : alTahunH) {
                            if (ketemuCocok) break;
                            for (String tahunT : alTahunT) {
                                if (tahunH.equals(tahunT)) {
                                    ketemuCocok = true;
                                    strTCocok = sT;
                                    totalSkor++;
                                    //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                                    break; //proses sH berikutnya
                                }
                            }
                        }
                    } else {
                       //tidak ketemu tahun di H, proses stringnya
                        if (sH.equals(sT)) {
                            ketemuCocok = true;
                            strTCocok = sT;
                            totalSkor++;
                            //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                            break; //proses sH berikutnya
                        }
                    }
                }   //for sT

                if (!ketemuCocok) { //kena penalti
                    double penalti = parameter.penaltiTgl;
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali * penalti;
                    //System.out.println("Tdk ada pasangan, kena penalti:"+sH+"("+penalti+")");
                } else  {
                   // System.out.println("Cocok: " + sH + "->" + strTCocok);
                }
            }
        }

        HashMap<String,String> hmVerbHT = new HashMap<>();



        //sisanya (kata)
        //perlu dicari nilai maksimum
        for (String vH:gtH.tokenKata) {
            double maxSkor = 0; //makin besar makin bagus, makin similar

            //hack: jika menggunakan konversi pasif ke aktif
            //
            if (vH.equals("subject")) {
                continue;
            }

            String strTercocok="";
            //loop untuk setiap T
            for (String vT:gtT.tokenKata) {
                if (vH.equals(vT)) {  //sama persis
                    maxSkor=1;
                    strTercocok = vT;

                    //System.out.println(vH+"->"+vT);
                    break; //tidak perlu diproses berikutnya
                } else
                    //bagian akronim UN dgn United atau Nations = 1 (belum)
                    //dua term berurutan cocok dengan single term (long term = long-term) (belum)
                    //simLSA, kalau disini diganti dengan w2vec atau glove
                    //booster wordnet
                    //tdk ada di vocab: karakter bigram, dice cooeficent, jika > 2/3 maka 1 else 0

                    //I,we, they,she  jika sama dgn obj pronoun (me, him, her) = 1
                    if (    (vH.equals("i")&&vT.equals("me"))    ||
                            (vH.equals("she")&&vT.equals("her"))  ||
                            (vH.equals("he")&&vT.equals("him"))   ||
                            (vH.equals("they")&&vT.equals("them"))
                            ) {
                        maxSkor=1;
                        strTercocok = vT;
                        break;
                        //System.out.println(vH+"->"+vT);
                    } else {
                        //cek skor kemiripan

                        double skorEm;


                        //cek apakah model disimpan dalam lowercase
                        String strH,strT;
                        if (!modelVec.isLowerCase) {
                            strH = vH.toLowerCase().trim();
                            strT = vT.toLowerCase().trim();
                        } else {
                            strH = vH.trim();
                            strT = vT.trim();
                        }

                        skorEm = modelVec.similarity(strH,strT);
                        if (skorEm > maxSkor) {
                            maxSkor = skorEm;
                            strTercocok = vT;
                        }

                        /*if (isGloveOrW2vec==0) {
                            System.out.println(vH.toLowerCase());
                            System.out.println(vT.toLowerCase());
                            double skorEmGlove;
                            try {
                                skorEmGlove = vecGlove.similarity(vH.toLowerCase().trim(), vT.toLowerCase().trim());
                            } catch (NullPointerException ex)  {
                                skorEmGlove = 0; //error null kalau tdk ketemu katanya
                            }
                            skorEm = skorEmGlove; //glove saja
                        }
                        else {
                            double skorW2V = vecW2V.similarity(vH,vT); //tidak dilowercase
                            skorEm = skorW2V; //w2vec saja
                        }
                        //System.out.println("Skor glove: "+vH+":"+vT+"="+skorEmGlove);

                        //double skorW2V = vecW2V.similarity(vH,vT); //tidak dilowercase
                        //double skorEm = (skorEmGlove+skorW2V) / 2; //rata22
                        //double skorEm = Math.max(skorEmGlove,skorW2V); //cari max
                        //double skorEm = Math.min(skorEmGlove,skorW2V); //cari min
                        //double skorEm = skorW2V; //w2vec saja
                        //double skorEm = skorEmGlove; //glove saja
                        if (skorEm > maxSkor) {
                            maxSkor = skorEm;
                            strTercocok = vT;
                        }
                        */

                    }
            } //loop for vT
            //System.out.print(vH +"-> "+strTercocok+" ");
            hmVerbHT.put(vH,strTercocok);
           // System.out.println("("+maxSkor+")");

            //terlalu rendah, kena penalti
            if (maxSkor<=parameter.batasPenaltiKata) {
               // System.out.println("vH kena penalti.");
                if ( itH.alNoun.contains(vH) ||
                        itH.alPronoun.contains(vH) ||
                        itH.alVerb.contains(vH)) {
                    //System.out.println(" Verb/noun/pronoun, penalti lebih besar");
                    double penalti = parameter.penaltiKataVerbNoun;
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali*penalti;

                } else {
                    //System.out.println("Selain Verb/noun/pronoun, penalti lebih kecil");
                    double penalti = parameter.penaltiKataLain;
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali*penalti;
                }
            } else {
                //PENGECEKAN SUBYEK
                //cek subyek VH dengan subyek strTercocok apakah sama?
                //kalau tidak sama harusnya kena penalti

                //hanya proses kalau vH adalah verb dan tidak kena penalti
                /*

                if (itH.alVerb.contains(vH)) {
                    boolean subjCocok = cekSubjCocok(vH, strTercocok);
                    if (subjCocok) {
                        System.out.println("Subyek cocok");
                    } else {
                        System.out.println("Subyek tidak cocok, penalti");
                        double penalti = penaltiSubjTdkCocok;
                        totalPenalti = totalPenalti+penalti;
                    }
                }
                //end pengecekan subyek
                */
            }

            /*
            //jika melebihi thresold tertentu tambah boosting wordnet
            //masih gagal, nggak ada kata wordnet yg ketemu

            //0.5 e ^ (-alpha* D(x,y)
            double alpha = 0.25;
            //rule 1: same wordnet sysnset (sdh)
            //rule 2: directy hypernym (sdh)
            double penambah=0;
            //tambah wordnet
            //pake batas penalti saja
            //tapi kalau=1 artinya sudah eksak, gak perlu ditambah lagis
            if (maxSkor>batasPenaltiKata && maxSkor<1) {
                //hanya kalau vh dan vt verb/noun  (nanti tambah adjektif?)
                if ( itH.alNoun.contains(vH.toLowerCase()) ||
                     itH.alVerb.contains(vH.toLowerCase()) ) {

                    System.out.println("tambah boost wordnet");
                    String lt  = pLemma.lemmatize(strTercocok.toLowerCase());
                    String lVh = pLemma.lemmatize(vH.toLowerCase());

                    POS posKata=null;

                    if (itH.alNoun.contains(vH.toLowerCase())) {
                        posKata = POS.NOUN;
                    } else if (itH.alVerb.contains(vH.toLowerCase())) {
                        posKata = POS.VERB;
                    }
                    if (pWordnet.isSatuSynset(posKata,lVh,lt)) {
                        //path = 0
                        //0.5 * e ^ -0.25*0
                        penambah = 0.5;
                        System.out.println("satu synset, ditambah 0.5");
                    } else if (pWordnet.isDirectHypernym(posKata,lVh,lt)) {
                        //path = 1
                        // 0.5 * e ^ -0.25*1 = 0.38940039153
                        penambah = 0.38940039153;
                        System.out.println("directy hypernim , ditambah ");
                    } else {
                        //nebak, path = 2
                        penambah = 0.30326532985;
                    }
                    System.out.println("Lh->Lt:"+lVh+"->"+lt);
                    System.out.println("Penambah:"+penambah);
                }  // noun / werb
            }
            */

            totalSkor = totalSkor + maxSkor;
            //totalSkorKali = totalSkorKali * maxSkor;
        }

        //hitung penalti kalau salah satu adalah kalimat negatif
        //atau kalimat tidak langsung


        CekKalimatNegatif ck = new CekKalimatNegatif(ppStopWords);
        StructCariKalNegatif tNeg = ck.isKalimatNegatif(itT);
        StructCariKalNegatif hNeg = ck.isKalimatNegatif(itH);


        double skorPenaltiNeg = 0;

        //hanya jika salah satu kalimat negatif XOR
        if (tNeg.isNegatif ^ hNeg.isNegatif)  {
            //hanya kalau verbnya sesuai

            //kasus yg paling umum
            //harus dicek apakah verb di H ada di bagian verb not
            //tapi mungkin harusnya dicek verb yang langsung berurutan!
            if ((tNeg.isNegatif) && hmVerbHT.containsValue(tNeg.verb))  {
                skorPenaltiNeg = parameter.penaltiKalNeg;
                //System.out.println("KENA PENALTI NEGATIF");
                //System.out.println("verb:"+ tNeg.verb);
            }

            //kalau sebaliknya susah, karena jumlah verb di H lebih banyak
            //diignore saja

        }



        int jumToken = gtH.tokenUang.size()+gtH.tokenTgl.size()+
                gtH.tokenAngka.size()+gtH.tokenKata.size(); //+gtH.tokenLokasi.size();

        //penalti negatif
        out = (totalSkor / jumToken) - (totalPenalti / jumToken) - skorPenaltiNeg; //tidak perlu dikali 2 karena hanya dari H->T (tidak bolakbalik)

        //tanpa penalti negatif
        //out = (totalSkor / jumToken) - (totalPenalti / jumToken); //tidak perlu dikali 2 karena hanya dari H->T (tidak bolakbalik)

        //out = (totalSkorKali / (jumToken)) - (totalPenaltiKali / (jumToken)); //tidak perlu dikali 2 karena hanya dari H->T (tidak bolakbalik)

        //penalti di jumlah, totalskor dikali
        //out = (totalSkorKali / (jumToken)) - (totalPenalti/ (jumToken));

        //System.out.println("Total skor:"+totalSkor/jumToken);
        //System.out.println("Total skor:"+totalSkorKali/jumToken);

        //System.out.println("Total penalti:"+totalPenalti/jumToken);
        //System.out.println("Total penalti:"+totalPenaltiKali/jumToken);

        return out;
    }



    //hitung sim antara gtH dan gtT, gtH dan gtT sudah diisi
    //proses kata per kata
    public double getSim(){
        double out = 0;
        out =  getSimInternal(gtTT,gtHH);
        return out;
    }

    public String getNearestNeighbor(String seed) {
        Collection<String> nw;
        nw =  modelVec.getNearestNeighbor(seed); //diambil 10
        /*
        if (isGloveOrW2vec==0) {
            nw =  vecGlove.wordsNearest(seed,10); //sebanyak sepuluh
        }
        else {
            nw =  vecW2V.wordsNearest(seed,10); //sebanyak sepuluh
        }
        */
        StringBuilder sb = new StringBuilder();
        for (String s: nw) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        //debug
        //iw.init("rte3_babak2","juli_glove_42B",0,);  //<-- terbaik
        ParameterSimGroupToken par = new ParameterSimGroupToken();
        SimGroupToken sgt;
        //sgt = new SimGroupToken(0,"D:\\eksperimen\\glove\\glove.42B.300d.txt",par);
        //String strModel = "D:\\eksperimen\\glove\\glove.6B.300d.txt";
        //String strModel = "D:\\eksperimen\\glove\\glove.42B.300d.txt";
        //String strModel = "D:\\eksperimen\\glove\\glove.840B.300d.txt";
        //String strModel ="D:\\eksperimen\\paragram\\paragram_300_sl999\\paragram_300_sl999\\paragram_300_sl999.txt";
        //String strModel ="D:\\eksperimen\\paragram\\paragram_300_ws353\\paragram_300_ws353\\paragram_300_ws353.txt";
        //glove: sgt = new SimGroupToken(0,strModel,par);

        //word2vec
        //String strModel ="D:\\eksperimen\\textualentailment\\GoogleNews-vectors-negative300.bin.gz";
        /*
        String strModel ="D:\\eksperimen\\baroni_vector\\EN-wform.w.5.cbow.neg10.400.subsmpl_v_spasi.txt";
        Prepro pp = new Prepro();
        pp.loadStopWords("stopwords2","kata");
        sgt = new SimGroupToken(0,strModel,par,pp);
        //sgt.parameter = par;


        ArrayList<String> alKata= new ArrayList<>();
        alKata.add("dagbladet");
        alKata.add("Dagbladet");
        alKata.add("resided");
        alKata.add("evacuated");
        alKata.add("defeated");
        alKata.add("drug");
        alKata.add("member");
        alKata.add("vegetation");
        alKata.add("improves");
        alKata.add("people");
        alKata.add("terminals");
        alKata.add("used");
        alKata.add("provide");
        alKata.add("asia");
        alKata.add("democratic");
        alKata.add("sets");
        alKata.add("new");
        alKata.add("opponent");
        //System.out.println(strModel);
        for (String kata: alKata) {
            String nn = sgt.getNearestNeighbor(kata);
            //System.out.println(kata+ ":" + nn);
        }
        */

        //System.out.println("-------------------");
        /*
        String t = "After his release, the clean-shaven Magdy el-Nashar told reporters outside his home that he had " +
                "nothing to do with the July 7 transit attacks, which killed 52 people and the four bombers.";
        String tNer = "PERSON=Magdy el-Nashar;DATE=July 7;NUMBER=52;NUMBER=four;";
        String h = "52 people and four bombers were killed on July 7.";
        String hNer = "NUMBER=52;NUMBER=four;DATE=July 7;";
        */

        /*
        String t = "Take consumer products giant Procter and Gamble. Even with a $1.8 billion Research and Development " +
                "budget, it still manages 500 active partnerships each year, many of them with small companies.";
        String h = "500 small companies are partners of Procter and Gamble.";

        GroupToken gtT = new GroupToken();
        gtT.ambilToken(t,tNer);

        GroupToken gtH = new GroupToken();
        gtH.ambilToken(h,hNer);

        */

        //D:\eksperimen\paragram\paragram_300_sl999\paragram_300_sl999\paragram_300_sl999.txt

        /*
        SimGroupToken sgt = new SimGroupToken("D:\\eksperimen\\glove\\glove.6B.300d.txt",
                "D:\\eksperimen\\textualentailment\\GoogleNews-vectors-negative300.bin.gz");


        sgt.setGroupToken(gtT,gtH);
        double skor = sgt.getSim();
        System.out.println("skor:"+skor);
        */
    }

}

/*

 //tanggal
        if (gtH.tokenTgl.size()>0) {
            for (String sH: gtH.tokenTgl) {
                boolean ketemuCocok = false;
                System.out.println("Cari tgl:"+sH);
                Span timeH = Chronic.parse(sH, optTime);  //Chronic jelek!! diubah hanya ambil tahun saja
                Calendar calH=null;
                if (timeH!=null) {
                    calH = timeH.getBeginCalendar();
                    System.out.println("cal H:" + calH.getTime());
                } else {
                    System.out.println("Gagal parsing date H:" + sH);
                }
                for (String sT : gtT.tokenTgl) {
                    //konversi tgl
                    Span timeT = Chronic.parse(sT, optTime);
                    Calendar calT = null;
                    if (timeT != null) {
                        calT = timeT.getBeginCalendar();
                        System.out.println("" + calT.getTime());
                    } else {
                        System.out.println("Gagal parsing date T:" + sT);
                    }

                    if ((calH != null) && (calT != null)) {
                        if (calH.equals(calT)) {
                            ketemuCocok = true;
                            totalSkor++;
                            //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                            System.out.println(sH + "->" + sT);
                            break; //proses sH berikutnya
                        }
                    } else {  //salah satu gagal di parse, gunakan perbandingan string saja
                        System.out.println("Gagal parse tgl, bandingkan stringnya");
                        if (sH.equals(sT)) {
                            ketemuCocok = true;
                            totalSkor++;
                            //totalSkorKali = totalSkorKali * 1; //cuma supaya jelas saja, bisa dibuang baris ini
                            System.out.println(sH + "->" + sT);
                            break; //proses sH berikutnya
                        }
                    }
                }   //for sT
                 //if timeH<>null

                if (!ketemuCocok) { //kena penalti
                    double penalti = 0.5;
                    totalPenalti = totalPenalti+penalti;
                    //totalPenaltiKali = totalPenaltiKali * penalti;
                    System.out.println("Tdk ada pasangan, kena penalti:"+sH+"("+penalti+")");
                }
            }
        }

 */
