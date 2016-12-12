package edu.upi.yudiwbs.textualentailment.babaklima;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.CoreMap;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * Created by yudiwbs on 11/27/2015.
 *
 *   buang selain kata benda dan kata kerja
 *
 */

public class Prepro {
    Logger logger = Logger.getLogger(Prepro.class.getName());
    public String dbName;
    public String userName;
    public String password;
    private ArrayList<String> alStopWords = new ArrayList<>();
    private StanfordCoreNLP pipeline=null;
    private StanfordCoreNLP pipelineSplitKata=null;
    private LexicalizedParser lp;  //untuk parsing PosTag dan Dep Parser


    /**
     *    loading model
     *
     */
    public void initPosTagDep() {
        //panggil sebelum lakukan parsing syntatic dan dependency
        //todo: kenapa error
        //lp = new LexicalizedParser("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz","-maxLength", "80", "-retainTmpSubcategories");
        lp = null; //biar kalo error ketahuan
    }

    public String[] parsePosTagDep(String sen) {
        //IS: initPosTagDep harus dipanggil lebih dulu
        //output: string[0] adalah syntatic
        //        string[1] adalah dependency tree
        //hasilnya sama dengan yg di online (lebih bagus)
        //lebih simpel

        String[] out = new String[2];
        String kata = sen;

        //todo: perbaiki nanti
        //Tree parseTree = lp.apply(kata);
        Tree parseTree  = null;  //biar error sekalian
        TreebankLanguagePack tlp = new PennTreebankLanguagePack();
        GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
        GrammaticalStructure gs = gsf.newGrammaticalStructure(parseTree);
        Collection<TypedDependency> tdl = gs.typedDependenciesCCprocessed();
        out[0] = parseTree.toString();
        out[1] = tdl.toString();

        return out;
    }

    /**
     *  memproses postag dan dependency tree menggunakan stanford parser
     *  dari input, outPostTag dan outDependency terisi
     *
     *
     * @param namaTabel
     * @param namaFieldId
     * @param namaFieldInput
     *
     */
    public void isiFieldPosTagDep (String namaTabel, String namaFieldId, String namaFieldInput,
                                   String namaFieldOutPosTag,String namaFieldOutDependency)

    {
        Connection conn=null;
        PreparedStatement pSel=null;
        PreparedStatement pUpd=null;

        ResultSet rs = null;
        KoneksiDB db = new KoneksiDB();
        try {
            logger.log(Level.INFO,"Mulai parsing postag + dependency tree");
            conn = db.getConn();
            initPosTagDep();
            pSel = conn.prepareStatement(String.format("select %s,%s from %s",namaFieldInput,namaFieldId,namaTabel)); //"select t,h,id from "+namaTabel);
            pUpd = conn.prepareStatement(String.format("update %s set %s=?,%s=? where %s=?",namaTabel,namaFieldOutPosTag,namaFieldOutDependency,namaFieldId));

            rs = pSel.executeQuery();
            while (rs.next()) {
                String text = rs.getString(1);
                int id = rs.getInt(2);
                System.out.println(id);
                String[] outT = parsePosTagDep(text);

                System.out.println(outT[0]);
                System.out.println(outT[1]);

                pUpd.setString(1, outT[0]);
                pUpd.setString(2, outT[1]);
                pUpd.setInt(3, id);
                try {
                    //todo aneh.. kena error terus karena kegedaan, coba dicatch dulu
                    pUpd.executeUpdate();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    System.out.println("error");
                    pUpd.setString(1, "error");
                    pUpd.setString(2, "error");
                    pUpd.setInt(3, id);
                    pUpd.executeUpdate();
                }
            }
            rs.close();
            pSel.close();
            conn.close();
            logger.log(Level.INFO,"selesai");
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }


    public void initLemma() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);
    }


    //dipanggil sebelum split token
    public void initSplitKalimat() {
        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit");
        pipelineSplitKata = new StanfordCoreNLP(props);
    }

    //initSplitKalimat harus dipanggil terlebih dulu
    public ArrayList<String> splitKalimat(String par) {

        ArrayList<String> alOut = new ArrayList<String>();
        Annotation docT = new Annotation(par);
        pipelineSplitKata.annotate(docT);
        List<CoreMap> sentencesT = docT.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap kalimat: sentencesT) {
            //System.out.println(kalimat.toString());
            alOut.add(kalimat.toString());
        }
        return alOut;
    }

    //IS: init lema sudah dipangggil!
    public String lemmatize(String documentText)
    {
        assert (pipeline!=null); //pastikan init sudah dipanggil
        List<String> lemmas = new LinkedList<>();
        // create an empty Annotation just with the given text
        Annotation document = new Annotation(documentText);
        // run all Annotators on this text
        this.pipeline.annotate(document);

        // Iterate over all of the sentences found
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for(CoreMap sentence: sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token: sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmas.add(token.get(CoreAnnotations.LemmaAnnotation.class));
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String s:lemmas) {
            sb.append(s+" ");
        }
        String out = sb.toString();
        //out = td.postProses(out);
        return out;
    }

    /*
        ambil jenis: "DATE", "NUMBER", "MONEY"

     */
    ArrayList<String> ambilSatuJenisNER(String jenis,String ner) {
        ArrayList<String> out = new ArrayList<>();
        HashMap<String,ArrayList<String>> hmTNer = ambilInfoNer(ner);
        for (String key : hmTNer.keySet()) {
            //filter key yg diinginkan
            if (key.equals(jenis)) {
                //ambil datanya
                ArrayList<String> alData = hmTNer.get(key);
                if (alData!=null) {
                    for (String s : alData) {
                        out.add(s);
                    }
                }
            }
        }
        return out;
    }




    /*
       ambil info ner dari database. Contoh inputnya: ORGANIZATION=Yukos;MONEY=US$ 27.5 billion;
       ORGANIZATION=Yuganskneftegaz;MONEY=US$ 9.4 billion;MISC=Baikalfinansgroup;MISC=Russian;ORGANIZATION=Rosneft;
       ORGANIZATION, MISC, DATE adalah key
       satu key bisa punya lebih dari satu value, disimpan di ArrayList

       contoh penggunaan:
       HashMap<String,ArrayList<String>> hmNer = pp.ambilInfoNer(ner);
       for (String key : hmNer.keySet()) {
            //filter key yg diinginkan
            if (  key.equals("PERSON") ||
                    key.equals("ORGANIZATION" )  ||
                    key.equals("LOCATION")  ) {

                //ambil datanya
                ArrayList<String> al = hmNer.get(key);
                for (String s:al) {
                }

    */

    public HashMap<String,ArrayList<String>> ambilInfoNer (String s) {
        HashMap<String,ArrayList<String>> out = new HashMap<>();
        Scanner sc = new Scanner(s);
        sc.useDelimiter(";");
        while (sc.hasNext()) {
            String kata = sc.next();
            String[] arrKata = kata.split("=");
            if (out.containsKey(arrKata[0])) {
                //sudah ada
                ArrayList<String> al = out.get(arrKata[0]);
                al.add(arrKata[1]);
            } else {
                ArrayList<String> al = new ArrayList<>();
                al.add(arrKata[1]);
                out.put(arrKata[0],al);
            }
        }
        return out;
    }

    public String loadKataTanpaStopWordstoString(String str,boolean keLowerCase,boolean buangSelainHuruf) {
        String out = "";
        ArrayList<String> alS = loadKataTanpaStopWords(str,keLowerCase,buangSelainHuruf);

        StringBuilder sb = new StringBuilder();
        for (String s:alS) {
            sb.append(s);
            sb.append(" ");
        }
        out = sb.toString().trim();
        return out;
    }

    //saya mau makan. => saya mau makan
    public String buangTitikDiAkhir(String str) {
        if (str != null && str.length() > 0 && str.charAt(str.length()-1)=='.') {
                str = str.substring(0, str.length()-1);
        }
        return str.trim();
    }

    /*
       hanya load ke arraylist biasa aja tanpa lewat stopwords
     */
    public ArrayList<String> loadKata(String str,boolean keLowerCase) {
        ArrayList<String> out = new  ArrayList<>();

        Scanner sc = new Scanner(str);
        while (sc.hasNext()) {
            String kata = sc.next().trim();
            if (keLowerCase) {
                kata = kata.toLowerCase();
            }
            out.add(kata);
        }
        return out;
    }



    //loadstopwords harus dipanggil terlebih dulu!!
    //hanya huruf yang diterima
    public ArrayList<String> loadKataTanpaStopWords(String str,boolean keLowerCase,boolean buangSelainHuruf) {
        assert (alStopWords.size()>0); //pastikan stopwords sudah diload
        ArrayList<String> out = new  ArrayList<>();
        String str2;
        if (buangSelainHuruf) {
            //buang selaing alphanumerik
            //underscore dibiarkan krn berguna untuk word2vec
            //update 26 mei, angka masuk saja
            str2 = str.replaceAll("[^A-Za-z0-9_]", " ").replaceAll("\\s+", " ").trim();
        } else {
            str2 = str; //tidak dibbuang
        }
        Scanner sc = new Scanner(str2);
        while (sc.hasNext()) {
            String kata = sc.next().trim();
            if (keLowerCase) {
                kata = kata.toLowerCase();
            }
            //buang kata yang ada di stopwords
            if (!alStopWords.contains(kata.toLowerCase())) {
                if (kata.length()>1) { //satu kar dibuang
                    out.add(kata);
                }
            }
        }
        return out;
    }

    public void loadStopWords(String namaTabel,String namaField) {
        //memindahkan data stopwords dari tabel ke memori alStopWords
        //FS: alStopWords terisi
        //System.out.println("loadStopWords");
        Connection conn=null;
        PreparedStatement pSel=null;
        KoneksiDB db = new KoneksiDB();
        alStopWords.clear();
        try {
            conn = db.getConn();
            pSel  = conn.prepareStatement (String.format("select id,%s from %s",namaField,namaTabel));
            ResultSet rs = pSel.executeQuery();
            int jumDiproses = 0;
            while (rs.next())  {
                String kata = rs.getString(2).trim();
                alStopWords.add(kata);
                //System.out.println(kata);
                jumDiproses++;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, null, e);
        }
        finally  {
            try  {
                if (pSel!= null) {pSel.close();}
                if (conn != null) {conn.close();}
            } catch (Exception e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, null, e);
            }
        }
    }

    //utility
    public void fileStopwordsToDB(String fileName,String tableName,String fieldName) {
              //fieldname: nama field di tabel stopwords

              //utility memindahkan isi file teks ke tabel
              //berguna untuk menambahkan data stopwords baru
              //melakukan pengecekan, kalau ada duplikasi maka tidak dimasukkan,
              // jadi tidak perlu dihapus sebelumnya


              System.out.println("filetodbstopwords");
              Connection conn=null;
              PreparedStatement pSdhAda=null;
              PreparedStatement pIns=null;
              int jumTdkDiproses=0;
              int jumDiproses=0;

              try {
                     Class.forName("com.mysql.jdbc.Driver");
                     KoneksiDB db = new KoneksiDB();
                     conn =  db.getConn();
                     pSdhAda = conn.prepareStatement     (" select id from  "+ tableName + " where "+ fieldName +" = ?");
                     pIns    =  conn.prepareStatement    (" insert into  "+ tableName + "("+fieldName+") values (?)");

                     FileInputStream fstream = new FileInputStream(fileName);
                     DataInputStream in = new DataInputStream(fstream);
                     BufferedReader br = new BufferedReader(new InputStreamReader(in));
                     String strLine;
                     ResultSet rs;
                     while ((strLine = br.readLine()) != null)   {
                            if (strLine.equals("")) {continue;}
                            //masuk ke tabel?
                            pSdhAda.setString(1,strLine);
                            rs = pSdhAda.executeQuery();
                            if (rs.next()) {
                                   //sudah ada, batalkan masuk
                                   jumTdkDiproses++;
                            } else {
                                   jumDiproses++;
                                   pIns.setString(1,strLine);
                                   pIns.executeUpdate();
                            }
                     }
              } catch (Exception e) {
                  e.printStackTrace();
                  logger.log(Level.SEVERE, null, e);
              }
              finally  {
                     try  {
                            if (pSdhAda != null) {pSdhAda.close();}
                            if (pIns != null)    {pIns.close();}
                            if (conn != null) {conn.close();}
                     } catch (Exception e) {
                            logger.log(Level.SEVERE, null, e);
                     }
              }
              System.out.println("selesai");
       }

       //menghasilkan infoteks yang didalamnya ada daftar verb, noun, pronoun
       //pangil loadStopWords terlebih dulu jika mau stopwords dihilangkan
       public InfoTeks isiInfoTeks(String strKalimat,String synTree)  {
           InfoTeks out = new InfoTeks();
           out.teksAsli = strKalimat;
           out.strukturSyn = synTree;

           //buang kata selain verb dan noun
           //buang kata yg ada di stopwords
           //menggunakan synctatic tree

           /*

           Contoh input:
           The sale was made to pay Yukos' US$ 27.5 billion tax bill, Yuganskneftegaz was originally sold for
           US$ 9.4 billion to a little known company Baikalfinansgroup which was later bought by the Russian
           state-owned oil company Rosneft .

           Contoh synctatic tree:
           (ROOT (S (S (NP (DT The) (NN sale)) (VP (VBD was) (VP (VBN made)
           (S (VP (TO to) (VP (VB pay) (NP (NP (NNP Yukos) (POS '))
           (ADJP (QP ($ US$) (QP (CD 27.5) (CD billion)))) (NN tax) (NN bill)))))))) (, ,)
           (NP (NNP Yuganskneftegaz)) (VP (VBD was) (ADVP (RB originally))
           (VP (VBN sold) (PP (IN for) (NP (QP ($ US$) (QP (CD 9.4) (CD billion)))))
           (PP (TO to) (NP (NP (DT a) (ADJP (RB little) (VBN known)) (NN company)
           (NN Baikalfinansgroup)) (SBAR (WHNP (WDT which)) (S (VP (VBD was) (ADVP (RB later))
           (VP (VBN bought) (PP (IN by) (NP (DT the) (JJ Russian) (JJ state-owned) (NN oil)
           (NN company) (NN Rosneft))))))))))) (. .)))

           //alstopwords terisi

           outputnya:
           Verb:was made pay was sold known was bought
           Noun:sale yukos tax bill yuganskneftegaz company baikalfinansgroup oil company rosneft
           pronoun:

           */

           //String strOut;
           //strOut = strIn.toLowerCase().replaceAll("[\\.]"," . ").replaceAll("'"," ' ").replaceAll(","," , ");   //casefolding, titik dibuat
                                                                             // ada spasi karena kata. <> kata
           //System.out.println(strOut);
           String  synTree2 = synTree.replaceAll("\\)", " ) ").toLowerCase(); //biar kurung tutup ngga lengket
           //System.out.println(synTree2);

           //strOut = strOut.replaceAll("[0-9()\"\\-.,]"," ");
           //strOut = strOut.replaceAll("[[^a-z ][\\-]]"," ");
           //proses stopwords
           //Scanner sc = new Scanner(strOut);
           StringBuilder sb = new StringBuilder();
           Scanner scTree = new Scanner (synTree2);

           StringBuilder sbVerb = new StringBuilder();
           StringBuilder sbNoun = new StringBuilder();
           int cc = 0;
           String lastTag="";
           String kata="";
           while (scTree.hasNext()) {
               String kataTree = scTree.next().trim();

               //cari sampai ketemu kata di synctree
               if (kataTree.contains("(")) {  //tag
                   lastTag = kataTree;
               } else {                      //nontag
                   kata = kataTree;
                   if (alStopWords.contains(kata) ||  kata.equals(")") ) {
                       continue;  //skip stop words
                   }
                   if (       lastTag.equals("(vbd") || lastTag.equals("(vbn") || lastTag.equals("(vb")
                           || lastTag.equals("(vbg") || lastTag.equals("(vbz") || lastTag.equals("(vbp") ) {
                       //verb
                       out.alVerb.add(kata);
                       //sbVerb.append(kata);
                       //sbVerb.append(" ");
                       //NNS Noun, plural 14. NNP Proper noun, singular 15. NNPS
                   } else if ( lastTag.equals("(nn")   || lastTag.equals("(nns") || lastTag.equals("(nnp")
                            || lastTag.equals("(nnps") ) {
                       //sbNoun.append(kata);
                       //sbNoun.append(" ");
                       out.alNoun.add(kata);
                   } else if ( lastTag.equals("(PRP")  || lastTag.equals("(PRP$")
                           || lastTag.equals("(WP")   || lastTag.equals("(WP$") ) {
                       //sbNoun.append(kata);
                       //sbNoun.append(" ");
                       out.alPronoun.add(kata);}
                   else {
                       //System.out.println("tag tdk ketemu"+lastTag);
                   }
                   sb.append(kata);
                   sb.append(" ");
               }
           }
           scTree.close();
           //out = sb.toString();

           //System.out.println("verb:"+sbVerb.toString());
           //System.out.println("noun:"+sbNoun.toString());
           return out;
       }




    /*
        token2 ini mungkin nanti dipisahkan ke kelas lain aja ya
        [0]: token
        [1]: kelimat sisa dikurangi token
    */

    public Object[] ambilTokenUang(String kal, String ner) {
        Object[] out = new Object[2];
        ArrayList<String> alToken = ambilSatuJenisNER("MONEY",ner);
        out[0] = alToken;

        //buang dari kalimat
        //perlu buang yg panjang lebih dulu
        //misal: August of 1799 harus dibuang duluan sebelum 1799
        //kalau terbalik maka akan ada sisa August of
        //mungkin harusnya ambil informasi posisi dari lib stanfordnya langsung lebih elegan
        ComparatorPanjangKalimat comparator = new ComparatorPanjangKalimat(false);
        Collections.sort(alToken, comparator);
        for (String t:alToken) {
            kal = kal.replaceAll(Pattern.quote(t),"");
        }
        out [1] = kal;
        return out;
    }

    /*
         token2 ini mungkin nanti dipisahkan ke kelas lain aja ya

          fix: normalisasi angka berbentuk huruf
        one
        out[0] ArrayList<STring> token angka
        out[1] STring kalimat sisa
     */
    public Object[] ambilTokenAngka(String kal, String ner) {
        Object[] out = new Object[2];
        ArrayList<String> alTokenNumber = ambilSatuJenisNER("NUMBER",ner);
        ArrayList<String> alTokenOrd = ambilSatuJenisNER("ORDINAL",ner);


        ArrayList<String> alTokenGab = new ArrayList<>();
        alTokenGab.addAll(alTokenNumber);
        alTokenGab.addAll(alTokenOrd);

        ArrayList<String> alToken2 = new ArrayList<>();


        //konversi kalau ada yg berbentuk one, two, three  one, second,third
        for (int i=0;i<alTokenGab.size();i++) {
            String s = alTokenGab.get(i).toLowerCase().trim();
            if (s.equals("one")||s.equals("first")||s.equals("1st")) {
                alToken2.add("1");
            } else
            if (s.equals("two")||s.equals("second")||s.equals("2nd")) {
                alToken2.add("2");
            } else
            if (s.equals("three")||s.equals("third")||s.equals("3th")) {
                alToken2.add("3");
            } else
            if (s.equals("four")||s.equals("fourth")||s.equals("4th")) {
                alToken2.add("4");
            } else if (s.equals("five")||s.equals("fifth")||s.equals("5th")) {
                alToken2.add("5");
            } else if (s.equals("six")||s.equals("sixth")||s.equals("6th")) {
                alToken2.add("6");
            } else if (s.equals("seven")||s.equals("seventh")||s.equals("7th")) {
                alToken2.add("7");
            } else if (s.equals("eight")||s.equals("eighth")||s.equals("8th")) {
                alToken2.add("8");
            } else if (s.equals("nine")||s.equals("ninth")||s.equals("9th")) {
                alToken2.add("9");
            } else if (s.equals("ten")||s.equals("tenth")||s.equals("10th")) {
                alToken2.add("10");
            } else {
                alToken2.add(s);
            }
        }
        out[0] = alToken2; //ambil yg sudah diproses

        //buang dari kalimat
        //perlu buang yg panjang lebih dulu
        //misal: August of 1799 harus dibuang duluan sebelum 1799
        //kalau terbalik maka akan ada sisa August of
        //mungkin harusnya ambil informasi posisi dari lib stanfordnya langsung lebih elegan
        ComparatorPanjangKalimat comparator = new ComparatorPanjangKalimat(false);
        Collections.sort(alTokenGab, comparator);
        for (String t:alTokenGab) {
            kal = kal.replaceAll(Pattern.quote(t),"");
        }
        out [1] = kal;
        return out;
    }



    /*
        berdasarkan kalimat dan ner, ambil token tanggal,
        lalu buang token tersebut dari kalimat
        output : [0] ArrayList<String>  berisi token
                 [1] String kalimat sisa

        perlu dilowercase, recently = Recently
     */
    public Object[] ambilTokenTgl(String kal, String ner) {
        Object[] out = new Object[2];
        ArrayList<String> alToken = ambilSatuJenisNER("DATE",ner);
        ArrayList<String> alToken2 = new ArrayList<>();
        for (String s:alToken) {
            alToken2.add(s.toLowerCase()); //jadikan lowercase
        }
        out[0] = alToken2;  //perlu lowercase karena misal: Recently dengan recently harusnya sama

        //buang dari kalimat
        //perlu buang yg panjang lebih dulu
        //misal: August of 1799 harus dibuang duluan sebelum 1799
        //kalau terbalik maka akan ada sisa August of

        ComparatorPanjangKalimat comparator = new ComparatorPanjangKalimat(false);

        Collections.sort(alToken, comparator);

        for (String t:alToken) {
            kal = kal.replaceAll(Pattern.quote(t),""); //harus tetap pake token bukan token2
        }
        out [1] = kal;

        return out;
    }

    /*
        berdasarkan kalimat dan ner, ambil token tanggal,
        lalu buang token tersebut dari kalimat
        output : [0] ArrayList<String>  berisi token
                 [1] String kalimat sisa

        perlu dilowercase, recently = Recently
     */
    public Object[] ambilTokenLokasi(String kal, String ner) {
        Object[] out = new Object[2];
        ArrayList<String> alToken = ambilSatuJenisNER("LOCATION",ner);
        ArrayList<String> alToken2 = new ArrayList<>();
        for (String s:alToken) {
            alToken2.add(s.toLowerCase()); //jadikan lowercase
        }
        out[0] = alToken2;  //perlu lowercase karena misal: Recently dengan recently harusnya sama

        //buang dari kalimat
        //perlu buang yg panjang lebih dulu
        //misal: August of 1799 harus dibuang duluan sebelum 1799
        //kalau terbalik maka akan ada sisa August of
        ComparatorPanjangKalimat comparator = new ComparatorPanjangKalimat(false);
        Collections.sort(alToken, comparator);
        for (String t:alToken) {
            kal = kal.replaceAll(Pattern.quote(t),""); //harus tetap pake token bukan token2
        }
        out [1] = kal;
        return out;
    }


       public static void main(String[] args) {
           Prepro pp = new Prepro();
           System.out.println(pp.buangTitikDiAkhir("Saya mau makan nasi."));
           //pp.loadStopWords("stopwords2","kata");
           //String out = pp.loadKataTanpaStopWordstoString("halo Bandung yudi's home air&air 123 and underscore_boleh",true,true);
           //String out = pp.loadKataTanpaStopWordstoString("George Herbert Walker Bush (born June 12, 1924) is the former 41st President of the United States of America. Almost immediately upon his return from the war in December 1944, George Bush married Barbara Pierce.",true,true);
           //System.out.println(out);

           //pp.loadStopWords("stopwords","kata");
           //ArrayList<String> out ;
           /*
           ArrayList<String> out = pp.loadKataTanpaStopWords("The sale was made to pay Yukos' " +
                   "US$ 27.5 billion tax bill, Yuganskneftegaz was originally sold for US$ 9.4 " +
                   "billion to a little known company Baikalfinansgroup which was later bought by" +
                   " the Russian state-owned oil company Rosneft",false);

           for (String s:out) {
               System.out.println(s);
           }
           System.out.println(""); */

           /*out = pp.loadKataTanpaStopWords("Actor Christopher_Reeve, best known for his role as Superman, " +
                   "is paralyzed and cannot breathe without the help of a respirator after breaking his neck in a " +
                   "riding accident in Culpeper, Va., on Saturday.",true,true);
           */

           /*out = pp.ambilTglNER("DATE=1799;MISC=French;PERSON=Louis Alexandre Berthier;LOCATION=Rome;PERSON=Pope Pius VI;LOCATION=Valence;LOCATION=Drôme;LOCATION=France;DATE=August of 1799");
           for (String s:out) {
               System.out.println(s);
           }
           */
           /*
           String kal = "As late as 1799, priests were still being imprisoned or deported to penal colonies and " +
                   "persecution only worsened after the French army led by General Louis Alexandre Berthier " +
                   "captured Rome and imprisoned Pope Pius VI, who would die in captivity in Valence, Drôme, " +
                   "France in August of 1799.";
           String ner = "DATE=1799;MISC=French;PERSON=Louis Alexandre Berthier;LOCATION=Rome;PERSON=Pope Pius VI;" +
                   "LOCATION=Valence;LOCATION=Drôme;LOCATION=France;DATE=August of 1799";
           Object[] objOut = pp.ambilTokenTgl(kal,ner);
           ArrayList<String> tokenTgl = (ArrayList<String>) objOut[0];
           String sisaKal = (String) objOut[1];

           for (String s:tokenTgl) {
               System.out.println(s);
           }

           System.out.println("kal:"+kal);
           System.out.println("Sisa kal:"+sisaKal);
           */

           //testing ambil token NER
           /*
           String kal = "Jerry Reinsdorf (born February 25 1936 in Brooklyn, New York) is the owner of Chicago White " +
                   "Sox and the Chicago Bulls. Recently, he helped the White Sox win the 2005 World Series and, in " +
                   "the process, collected his seventh championship ring overall (the first six were all with the " +
                   "Bulls in the 1990s), becoming the third owner in the history of North American sports to win a " +
                   "championship in two different sports.";
           String ner = "PERSON=Jerry Reinsdorf;DATE=February 25 1936;LOCATION=Brooklyn;LOCATION=New York;" +
                   "ORGANIZATION=Chicago White Sox;ORGANIZATION=Chicago Bulls;DATE=Recently;" +
                   "ORGANIZATION=White Sox;DATE=2005;MISC=World Series;ORDINAL=seventh;" +
                   "ORDINAL=first;NUMBER=six;ORGANIZATION=Bulls;DATE=the 1990s;ORDINAL=third;" +
                   "MISC=North American;NUMBER=two;";

           Object[] objOut = pp.ambilTokenAngka(kal,ner);
           ArrayList<String> arrToken = (ArrayList<String>) objOut[0];
           String sisaKal = (String) objOut[1];
           for (String s:arrToken) {
               System.out.println(s);
           }
           System.out.println("kal:"+kal);
           System.out.println("Sisa kal:"+sisaKal);
           */
           //pp.fileStopwordsToDB("D:\\desertasi\\eksperimen\\stopwords_washedu.txt","stopwords2","kata");

           //test ambil multikalimat dalam satu paragraph
           /*
           pp.initSplitKalimat();
           ArrayList<String> alKal = pp.splitKalimat("The Prime Minister of Spain Zapatero visited Brazil, Argentina, Chile and Uruguay recently, in a effort to build a left axis in South America. The cited countries' South American Presidents agreed to collaborate at international level, particularly in the United Nations , European Union and with Paris, Berlin and Madrid.");
           for (String s:alKal) {
               System.out.println(s);
           }
           */

           //test parse postTag dan dep parser
           /*
           pp.initPosTagDep();
           String[] hasil  =  pp.parse("Subject have recorded as much as 200 mm of rain in portions of British Columbia on the west coast of Canada since Monday.");
           System.out.println("syn:"+hasil[0]);
           System.out.println("dep:"+hasil[1]);
           */

           //isi postag dan dep parser untuk field yang sudah dinormalisasi
           //pp.isiFieldPosTagDep("rte3_babak3","id","t_normal","t_normal_gram_structure","t_normal_type_dependency");
           //pp.isiFieldPosTagDep("rte3_babak3","id","h_normal","h_normal_gram_structure","h_normal_type_dependency");
           //pp.isiFieldPosTagDep("rte3_test_normal","id","t","t_gram_structure","t_type_dependency");
       }
}
