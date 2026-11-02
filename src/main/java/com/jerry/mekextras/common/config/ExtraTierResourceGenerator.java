package com.jerry.mekextras.common.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;

/**
 * Converts explicit texture/model patterns from tiers.json into a low-priority resource pack. The generator does not
 * synthesize artwork: it maps configured asset paths to the exact files underneath config/mekanism_extras/assets.
 */
public final class ExtraTierResourceGenerator {

    private ExtraTierResourceGenerator() {
    }

    public static File getPackDirectory(File configRoot) {
        return new File(configRoot, "resourcepack");
    }

    public static void generate(File tierFile, File configRoot) throws Exception {
        File packRoot = getPackDirectory(configRoot);
        File assets = new File(packRoot, "assets/mekanism_extras");
        Files.createDirectories(assets.toPath());
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(tierFile.toPath(), StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(reader).getAsJsonObject();
        }
        JsonElement tiersElement = root.get("tiers");
        if (tiersElement == null || !tiersElement.isJsonArray()) return;
        clearGenerated(assets);
        for (JsonElement tierElement : tiersElement.getAsJsonArray()) {
            JsonObject tier = tierElement.getAsJsonObject();
            String id = get(tier, "id", "");
            if (id.isEmpty()) continue;
            boolean configurable = Boolean.parseBoolean(get(tier, "configurableBlockModels", "false"));
            copyFiles(tier, "textures", configRoot, assets, "textures/");
            copyFiles(tier, "models", configRoot, assets, "models/");
            if (configurable) {
                writeBlockState(assets, tier);
                writeStorageStates(assets, tier);
                String led = get(tier, "ledTexture", "");
                if (validResource(led)) {
                    writeLedModel(assets, id, led);
                    writeMoreMachineStates(assets, id, tier, led);
                }
            }
            JsonObject itemTextures = object(tier, "itemTextures");
            if (itemTextures != null && Boolean.parseBoolean(get(tier, "configurableItemModels", "true"))) {
                for (Map.Entry<String, JsonElement> entry : itemTextures.entrySet()) {
                    writeItemModel(assets, id, entry.getKey(), entry.getValue().getAsString());
                }
            }
        }
        File packMeta = new File(packRoot, "pack.mcmeta");
        Files.write(packMeta.toPath(), "{\"pack\":{\"pack_format\":1,\"description\":\"Mekanism Extras Legacy configured tiers\"}}"
              .getBytes(StandardCharsets.UTF_8));
    }

    private static void writeBlockState(File assets, JsonObject tier) throws Exception {
        String id = get(tier, "id", "");
        String machine = get(tier, "machineTexture", "");
        if (id.isEmpty() || machine.isEmpty() || machine.startsWith("file:")) return;
        String active = get(tier, "activeMachineTexture", machine);
        String led = get(tier, "ledTexture", "");
        StringBuilder state = new StringBuilder();
        state.append("{\"forge_marker\":1,\"defaults\":{\"model\":\"mekanism_extras:factory_led_overlay\",")
              .append("\"textures\":{\"front\":\"").append(machine).append("\",\"sides\":\"").append(machine)
              .append("\",\"east\":\"").append(machine).append("\",\"west\":\"").append(machine)
              .append("\",\"south\":\"").append(machine).append("\",\"up\":\"").append(machine)
              .append("\",\"down\":\"mekanism:blocks/factory/factory_bottom\",\"particle\":\"").append(machine)
              .append("\",\"led\":\"").append(led).append("\"}},")
              .append("\"variants\":{\"facing\":{\"north\":{},\"east\":{\"y\":90},\"south\":{\"y\":180},")
              .append("\"west\":{\"y\":-90},\"up\":{\"x\":-90},\"down\":{\"x\":90}},")
              .append("\"active\":{\"true\":{\"textures\":{\"front\":\"").append(active)
              .append("\",\"sides\":\"").append(active).append("\",\"east\":\"").append(active)
              .append("\",\"west\":\"").append(active).append("\",\"south\":\"").append(active)
              .append("\",\"up\":\"").append(active).append("\"}},\"false\":{}},")
              .append("\"inventory\":[{}]}}");
        File target = new File(assets, "blockstates/" + id + "_machine.json");
        Files.createDirectories(target.getParentFile().toPath());
        Files.write(target.toPath(), state.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void writeLedModel(File assets, String id, String led) throws Exception {
        File target = new File(assets, "models/block/" + id + "_machine_led.json");
        String source = "{\"parent\":\"mekanism_extras:factory_led_overlay\",\"textures\":{\"led\":\"" + led + "\"}}";
        Files.createDirectories(target.getParentFile().toPath());
        Files.write(target.toPath(), source.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeStorageStates(File assets, JsonObject tier) throws Exception {
        String id = get(tier, "id", "");
        if (id.isEmpty()) return;
        String cube = get(tier, "energyCubeTexture", "mekanism:blocks/models/energy_cube");
        String tank = get(tier, "fluidTankTexture", "mekanism:blocks/models/fluid_tank");
        if (validResource(cube)) {
            writeSimpleState(assets, id + "_energy_cube", "mekanism:energy_cube",
                  "all", cube, "particle", cube);
        }
        if (validResource(tank)) {
            String json = "{\"forge_marker\":1,\"defaults\":{\"model\":\"mekanism:fluid_tank\",\"textures\":{"
                  + "\"glass\":\"mekanism:blocks/models/fluid_tank_side_off\",\"frame\":\"" + tank
                  + "\",\"particle\":\"" + tank + "\"}},"
                  + "\"variants\":{\"facing\":{\"north\":{},\"east\":{\"y\":90},\"south\":{\"y\":180},"
                  + "\"west\":{\"y\":-90},\"up\":{\"x\":-90},\"down\":{\"x\":90}},"
                  + "\"active\":{\"true\":{\"textures\":{\"glass\":\"mekanism:blocks/models/fluid_tank_side_on\"}},"
                  + "\"false\":{}},\"inventory\":[{}]}}";
            write(assets, "blockstates/" + id + "_fluid_tank.json", json);
        }
    }

    private static void writeSimpleState(File assets, String id, String model, String key, String first,
                                         String secondKey, String second) throws Exception {
        String json = "{\"forge_marker\":1,\"defaults\":{\"model\":\"" + model + "\",\"textures\":{\""
              + key + "\":\"" + first + "\",\"" + secondKey + "\":\"" + second + "\"}},"
              + "\"variants\":{\"facing\":{\"north\":{},\"east\":{\"y\":90},\"south\":{\"y\":180},"
              + "\"west\":{\"y\":-90},\"up\":{\"x\":-90},\"down\":{\"x\":90}},"
              + "\"active\":{\"true\":{},\"false\":{}},\"inventory\":[{}]}}";
        write(assets, "blockstates/" + id + ".json", json);
    }

    private static void write(File assets, String path, String content) throws Exception {
        File target = new File(assets, path);
        Files.createDirectories(target.getParentFile().toPath());
        Files.write(target.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeMoreMachineStates(File assets, String id, JsonObject tier, String led) throws Exception {
        String machine = get(tier, "machineTexture", "");
        if (machine.isEmpty() || machine.startsWith("file:")) return;
        String active = get(tier, "activeMachineTexture", machine);
        for (String kind : new String[]{"chemical_infuser", "chemical_oxidizer", "chemical_washer",
              "crystallizer", "dissolution_chamber", "isotopic_centrifuge", "nutritional_liquifier", "organic_farm"}) {
            String model = modelFor(kind);
            String json = "{\"forge_marker\":1,\"defaults\":{\"model\":\"" + model + "\",\"textures\":{"
                  + "\"all\":\"" + machine + "\",\"all1\":\"" + machine + "\",\"front\":\"" + machine
                  + "\",\"sides\":\"" + machine + "\",\"particle\":\"" + machine + "\",\"led\":\"" + led + "\"}},"
                  + "\"variants\":{\"facing\":{\"north\":{},\"east\":{\"y\":90},\"south\":{\"y\":180},"
                  + "\"west\":{\"y\":-90},\"up\":{\"x\":-90},\"down\":{\"x\":90}},"
                  + "\"active\":{\"true\":{\"textures\":{\"all\":\"" + active + "\",\"all1\":\"" + active
                  + "\",\"front\":\"" + active + "\",\"sides\":\"" + active + "\"}},\"false\":{}},"
                  + "\"inventory\":[{}]}}";
            File target = new File(assets, "blockstates/" + id + "_" + kind + ".json");
            Files.createDirectories(target.getParentFile().toPath());
            Files.write(target.toPath(), json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String modelFor(String kind) {
        switch (kind) {
            case "chemical_infuser": return "mekceumoremachine:chemical_infuser";
            case "chemical_oxidizer": return "mekceumoremachine:chemical_oxidizer";
            case "chemical_washer": return "mekanism_extras:chemical_washer_led_overlay";
            case "crystallizer": return "mekceumoremachine:factory_crystallizer";
            case "organic_farm": return "mekceumoremachine:factory_organic_farm";
            default: return "mekanism_extras:chemical_dissolution_chamber_model";
        }
    }

    private static void writeItemModel(File assets, String tier, String kind, String texture) throws Exception {
        if (!texture.contains(":")) {
            texture = "mekanism_extras:" + texture;
        }
        String name;
        switch (kind) {
            case "installer": name = tier + "_tier_installer"; break;
            case "circuit": name = tier + "_control_circuit"; break;
            case "alloy":
                switch (tier) {
                    case "absolute": name = "alloy_radiance"; break;
                    case "supreme": name = "alloy_thermonuclear"; break;
                    case "cosmic": name = "alloy_shining"; break;
                    case "infinite": name = "alloy_spectrum"; break;
                    default: return;
                }
                break;
            default: name = kind; break;
        }
        String json = "{\"parent\":\"item/generated\",\"textures\":{\"layer0\":\"" + texture + "\"}}";
        File target = new File(assets, "models/item/" + name + ".json");
        Files.createDirectories(target.getParentFile().toPath());
        Files.write(target.toPath(), json.getBytes(StandardCharsets.UTF_8));
    }

    private static void copyFiles(JsonObject tier, String section, File configRoot, File assets, String destination)
          throws Exception {
        JsonObject entries = object(tier, section);
        if (entries == null) return;
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) continue;
            String source = entry.getValue().getAsString();
            if (!source.startsWith("file:")) continue;
            String relative = source.substring("file:".length()).replace('\\', '/');
            File sourceFile = new File(configRoot, relative);
            if (!sourceFile.isFile()) continue;
            String key = entry.getKey().replace(':', '/');
            String extension = extension(sourceFile);
            if (!key.endsWith(extension)) key += extension;
            File target = new File(assets, destination + key);
            Files.createDirectories(target.getParentFile().toPath());
            Files.copy(sourceFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String extension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }

    private static String get(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static void clearGenerated(File assets) throws Exception {
        for (String directory : new String[]{"blockstates", "models/item"}) {
            File target = new File(assets, directory);
            if (!target.isDirectory()) continue;
            File[] children = target.listFiles();
            if (children == null) continue;
            for (File child : children) {
                Files.deleteIfExists(child.toPath());
            }
        }
    }

    private static boolean validResource(String value) {
        return value != null && !value.isEmpty() && !value.startsWith("file:");
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
