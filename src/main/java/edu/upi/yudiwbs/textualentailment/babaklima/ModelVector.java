package edu.upi.yudiwbs.textualentailment.babaklima;

import org.deeplearning4j.nn.api.Model;

import java.util.Collection;

/**
 * Created by yudiwbs on 11/12/2016.
 *  -kelas abstrak model vector yang digunakan  oleh SimGroupToken
 *  -nanti digunakan untuk word2vec maupun glove
 *  -sebelumnya digabung sulit
 *
 */
public abstract class ModelVector {
    protected boolean isLowerCase;  //semua kata dalam model dalam lowercase

    abstract public Collection<String> getNearestNeighbor(String seed);
    abstract double similarity(String kata1, String kata2);

    boolean getIsLowerCase() {
        return isLowerCase;
    }

    abstract void loadModel(String namaFile);

}
