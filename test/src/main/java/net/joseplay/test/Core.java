package net.joseplay.test;

import net.joseplay.test.config.PluginSettings;
import net.joseplay.test.couple.Couple;
import net.joseplay.test.feature.FeatureRegistry;
import net.joseplay.test.feature.defaults.HeartsFeature;
import net.joseplay.test.feature.defaults.TextFeature;
import net.joseplay.test.storage.CouplesRepository;
import net.joseplay.test.storage.Database;
import net.joseplay.test.storage.FeatureRepository;
import net.joseplay.test.storage.impls.CouplesImpl;
import net.joseplay.test.storage.impls.FeaturesImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

public class Core {

    private final FeatureRegistry featureRegistry = new FeatureRegistry();
    private final CouplesRepository couplesRepository;
    private final FeatureRepository featureRepository;
    private final Database dataBase;
    private final PluginSettings pluginSettings = new PluginSettings();

    public Core() {
        this.dataBase = new Database(pluginSettings);

        dataBase.start();

        this.featureRepository = new FeaturesImpl(dataBase);
        this.couplesRepository = new CouplesImpl(this);
    }

    public void onEnable() {
        featureRegistry.register(new HeartsFeature());
        featureRegistry.register(new TextFeature());

        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

//        try {
//            System.out.println("UUID 1" + p1.toString());
//            System.out.println("UUID 2" + p2.toString());
//
//            Couple couple = couplesRepository.create(p1, p2);
//
//            couple.features().set("text", "Hoje é " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss")));
//
//
//            Thread.sleep(2000);
//
//            System.out.println(Optional.of(couple.features().get("text")).orElse("Ngc é nulo"));
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        Couple couple = couplesRepository.find(
                UUID.fromString("51c73b37-417c-4c7d-8b9b-70d4b0572e24"),
                UUID.fromString("45e8a0c6-9d8c-4cbd-bf3b-a266b062bf5c")
        ).orElse(null);

        if (couple == null) System.out.println("couple nao encontrado");

        System.out.printf(couple.features().values().toString());



    }

    public void onDisable() {
        dataBase.shutdown();
    }

    public FeatureRegistry getFeatureRegistry() {
        return featureRegistry;
    }

    public CouplesRepository getCouplesRepository() {
        return couplesRepository;
    }

    public FeatureRepository getFeatureRepository() {
        return featureRepository;
    }

    public Database getDataBase() {
        return dataBase;
    }

    public PluginSettings getPluginSettings() {
        return pluginSettings;
    }

    public static void main(String[] args) {
        Core core = new Core();

        core.onEnable();
    }
}