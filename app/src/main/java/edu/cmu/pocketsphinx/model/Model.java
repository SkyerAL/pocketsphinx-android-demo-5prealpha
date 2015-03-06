package edu.cmu.pocketsphinx.model;

/**
 * Модель
 *
 * Created by Green on 18.02.2015.
 */
public class Model {

    private static ModelData data = new ModelData();

    private static ModelState currentState = ModelState.STOP;

    public static boolean setState(ModelState state) {
        if (state == ModelState.ACTIVATE && currentState == ModelState.STOP) {
            data.init();
            currentState = state;
            return true;
        }
        else
        if (state == ModelState.START && currentState == ModelState.ACTIVATE) {
            currentState = state;
            return true;
        }
        else
        if (state == ModelState.STOP) {
            currentState = state;
            return true;
        }
        return false;
    }

    public static ModelState getState() {
        return currentState;
    }

    public static ModelData getData() {
        return data;
    }
}
