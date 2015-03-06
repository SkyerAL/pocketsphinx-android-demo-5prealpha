package edu.cmu.pocketsphinx.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Данные модели (набор артикулов)
 *
 * Created by Green on 18.02.2015.
 */
public class ModelData {

    private List<Article> articles;

    private int currentIndex;

    public ModelData() {
        super();
    }

    /**
     * Загрузить список артикулов
     */
    public void init() {
        // load some
        articles = new ArrayList<>();
        Random random = new Random(System.currentTimeMillis());
        for (int i=0;i<10;i++) {
            articles.add(new Article(String.valueOf(random.nextInt(999) + 1)));
        }

        currentIndex = 0;
    }

    /**
     * Получить следующий артикул
     * @return артикул
     */
    public Article getNext() {
        currentIndex++;
        return getCurrent();
    }

    /**
     * Получить текущий артикул
     * @return артикул
     */
    public Article getCurrent() {
        if (articles != null && !articles.isEmpty() && currentIndex < articles.size()) {
            return articles.get(currentIndex);
        }
        return null;
    }
}
