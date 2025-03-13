package io.github.barmoury.api;

import io.github.barmoury.api.model.Model;
import io.github.barmoury.api.model.modelling.IdModel;

import java.util.Date;

public class Timeo {

    public static void resolve(Model model) {
        if (model instanceof IdModel idModel) {
            if (idModel.getId() == null) { resolveCreated(model); }
        }
        else { resolveUpdated(model); }
    }

    static void resolveCreated(Model model) {
        model.setCreatedAt(new Date());
        resolveUpdated(model);
    }

    static void resolveUpdated(Model model) {
        model.setUpdatedAt(new Date());
    }

}
