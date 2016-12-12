package edu.upi.yudiwbs.textualentailment.babaklima;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 *   Created by yudiwbs on 11/12/2016.
 *   model w2vec yang ditraining sendiri
 *
 */
public class ModelVectorW2VecCustom extends ModelVector {
    Word2Vec vec  = null;

    ModelVectorW2VecCustom() {
        isLowerCase = true;
    }

    @Override
    public Collection<String> getNearestNeighbor(String seed) {
        return  vec.wordsNearest(seed,10); //sebanyak sepuluh
    }

    //model sudah diload
    @Override
    double similarity(String kata1, String kata2) {
        double sim = 0;

        try {
            sim = vec.similarity(kata1,kata2);
        } catch (NullPointerException ex)  {
            sim = 0; //error null kalau tdk ketemu katanya
        }
        return sim;
    }

    @Override
    void loadModel(String namaFile) {

        System.out.println("Load w2vec custom training sendiri.. mungkin lama");
        System.out.println("Nama file model:"+namaFile);
        try {
            vec = WordVectorSerializer.readWord2Vec(new File(namaFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
