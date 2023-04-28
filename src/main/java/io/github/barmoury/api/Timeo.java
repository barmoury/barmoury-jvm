package io.github.barmoury.api;

import io.github.barmoury.api.model.Model;

import java.util.Date;

public class Timeo {

    public static void resolve(Model model) {
        if (model.getId() == 0) { resolveCreated(model); }
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
