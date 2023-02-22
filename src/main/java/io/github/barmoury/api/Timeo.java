package io.github.barmoury.api;

import io.github.barmoury.api.model.BarmouryModel;

import java.util.Date;

public class Timeo {

    public static void resolve(BarmouryModel barmouryModel) {
        if (barmouryModel.getId() == 0) { resolveCreated(barmouryModel); }
        else { resolveUpdated(barmouryModel); }
    }

    static void resolveCreated(BarmouryModel barmouryModel) {
        barmouryModel.setCreatedAt(new Date());
        resolveUpdated(barmouryModel);
    }

    static void resolveUpdated(BarmouryModel barmouryModel) {
        barmouryModel.setUpdatedAt(new Date());
    }

}
