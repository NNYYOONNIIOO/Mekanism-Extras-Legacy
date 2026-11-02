package com.jerry.mekextras.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jerry.mekextras.api.tier.AdvancedTier;
import mekanism.api.EnumColor;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExtraConfig {

    public static Configuration config;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<AdvancedTier, TierSpec> TIER_SPECS = new EnumMap<>(AdvancedTier.class);
    private static File configDirectory;

    public static boolean enableNaquadahOre;
    public static int naquadahSmallVeinsPerChunk;
    public static int naquadahSmallMinHeight;
    public static int naquadahSmallMaxHeight;
    public static int naquadahSmallVeinSize;
    public static boolean enableEndNaquadahOre;
    public static int endNaquadahVeinsPerChunk;
    public static int endNaquadahMinHeight;
    public static int endNaquadahMaxHeight;
    public static int endNaquadahVeinSize;
    public static boolean enableConfigurableTiers;
    public static boolean reloadTiersOnConfigChange;
    public static String tierConfigPath;

    public static void init(FMLPreInitializationEvent event) {
        File legacyConfig = new File(event.getModConfigurationDirectory(), "mekanism_extras.cfg");
        configDirectory = new File(event.getModConfigurationDirectory(), "mekanism_extras");
        if (!configDirectory.exists() && !configDirectory.mkdirs()) {
            throw new IllegalStateException("Could not create configuration directory " + configDirectory);
        }
        config = new Configuration(new File(configDirectory, "core.cfg"));
        config.load();
        sync();
        if (legacyConfig.isFile() && !new File(configDirectory, "core.cfg").equals(legacyConfig)) {
            try {
                File migrated = new File(configDirectory, "mekanism_extras.cfg.legacy");
                if (!migrated.exists()) {
                    Files.move(legacyConfig.toPath(), migrated.toPath(),
                          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (Exception ignored) {
            }
        }
        reloadTierConfig();
    }

    public static File getConfigDirectory() {
        return configDirectory;
    }

    public static JsonObject getTemplate() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        JsonArray tiers = new JsonArray();
        tiers.add(tierJson("absolute", "Absolute", "e8e983", false, null, "absolute", "absolute",
              "mekanism_extras:absolute_tier_installer", "mekanism_extras:absolute_control_circuit",
              "mekanism_extras:alloy_radiance"));
        tiers.add(tierJson("supreme", "Supreme", "e65249", false, null, "supreme", "supreme",
              "mekanism_extras:supreme_tier_installer", "mekanism_extras:supreme_control_circuit",
              "mekanism_extras:alloy_thermonuclear"));
        JsonObject cosmic = tierJson("cosmic", "Cosmic", "facef7", true,
              new String[]{"f516ef", "f761f2", "f99df5", "fac0f6", "facef7"}, "cosmic", "cosmic",
              "mekanism_extras:cosmic_tier_installer", "mekanism_extras:cosmic_control_circuit",
              "mekanism_extras:alloy_shining");
        cosmic.addProperty("frameTicks", 6.0F);
        cosmic.add("paletteOrder", intArray(0, 1, 2, 3, 4, 3, 2, 1));
        tiers.add(cosmic);
        JsonObject infinite = tierJson("infinite", "Infinite", "dfd9ab", true,
              new String[]{"dcc0ab", "d6a1b8", "cf9fd5", "a9b0dc", "a9d5c7", "dfd9ab"}, "infinite", "infinite",
              "mekanism_extras:infinite_tier_installer", "mekanism_extras:infinite_control_circuit",
              "mekanism_extras:alloy_spectrum");
        infinite.addProperty("frameTicks", 7.0F);
        infinite.add("paletteOrder", intArray(0, 1, 2, 3, 4, 5));
        tiers.add(infinite);
        tiers.add(lunaTierJson());
        root.add("tiers", tiers);
        JsonObject transmitters = new JsonObject();
        for (String type : new String[]{"universal_cable", "mechanical_pipe", "pressurized_tube",
              "thermodynamic_conductor", "logistical_transporter"}) {
            JsonObject definition = new JsonObject();
            definition.addProperty("blockPattern", "mekanism_extras:{tier}_" + type);
            definition.addProperty("modelPattern", "mekanism_extras:block/{tier}_" + type);
            definition.addProperty("texturePattern", "mekanism_extras:blocks/transmitter/{tier}_" + type);
            transmitters.add(type, definition);
        }
        transmitters.addProperty("note",
              "Patterns are documentation for the built-in transmitter model family; block and texture replacements use blocks/textures.");
        root.add("transmitters", transmitters);
        return root;
    }

    public static void sync() {
        //These keys were generated before world generation was aligned with the upstream mod.
        config.getCategory("world").remove("naquadahOreVeinsPerChunk");
        config.getCategory("world").remove("naquadahOreMaxHeight");
        Property oreEnabled = config.get("world", "enableNaquadahOre", true, "Enable naquadah ore generation in the overworld.");
        Property smallVeins = config.get("world", "naquadahSmallVeinsPerChunk", 8, "Number of small naquadah veins generated per overworld chunk.", 0, 64);
        Property smallMin = config.get("world", "naquadahSmallMinHeight", 1, "Minimum Y level for small naquadah veins. The upstream mod uses the bottom bedrock range.", 1, 255);
        Property smallMax = config.get("world", "naquadahSmallMaxHeight", 4, "Maximum Y level for small naquadah veins.", 1, 255);
        Property smallSize = config.get("world", "naquadahSmallVeinSize", 4, "Maximum number of blocks in a small naquadah vein.", 1, 32);
        Property endOre = config.get("world", "enableEndNaquadahOre", true, "Enable naquadah ore generation in the end.");
        Property endVeins = config.get("world", "endNaquadahVeinsPerChunk", 24, "Number of naquadah veins generated per End chunk.", 0, 64);
        Property endMin = config.get("world", "endNaquadahMinHeight", 10, "Minimum Y level for End naquadah veins.", 1, 255);
        Property endMax = config.get("world", "endNaquadahMaxHeight", 30, "Maximum Y level for End naquadah veins.", 1, 255);
        Property endSize = config.get("world", "endNaquadahVeinSize", 6, "Maximum number of blocks in an End naquadah vein.", 1, 32);
        Property tierConfig = config.get("tiers", "enableConfigurableTiers", true,
              "Read tier definitions from tiers.json. Disabled configurations fall back to the built-in defaults.");
        Property reloadTiers = config.get("tiers", "reloadTiersOnConfigChange", false,
              "Reload tiers.json after a Forge configuration refresh. Resource-pack textures are reloaded separately.");
        Property tierPath = config.get("tiers", "tierConfigPath", "tiers.json",
              "Path relative to config/mekanism_extras, or an absolute path.");
        Property regenerate = config.get("tiers", "regenerateTierTemplate", false,
              "Set true and reload the game to overwrite tiers.json with the current complete template.");

        enableNaquadahOre = oreEnabled.getBoolean();
        naquadahSmallVeinsPerChunk = smallVeins.getInt();
        naquadahSmallMinHeight = smallMin.getInt();
        naquadahSmallMaxHeight = Math.max(naquadahSmallMinHeight, smallMax.getInt());
        naquadahSmallVeinSize = smallSize.getInt();
        enableEndNaquadahOre = endOre.getBoolean();
        endNaquadahVeinsPerChunk = endVeins.getInt();
        endNaquadahMinHeight = endMin.getInt();
        endNaquadahMaxHeight = Math.max(endNaquadahMinHeight, endMax.getInt());
        endNaquadahVeinSize = endSize.getInt();
        enableConfigurableTiers = tierConfig.getBoolean();
        reloadTiersOnConfigChange = reloadTiers.getBoolean();
        tierConfigPath = tierPath.getString();
        if (regenerate.getBoolean()) {
            regenerate.set(false);
            if (config.hasChanged()) {
                config.save();
            }
            File file = getTierConfigFile();
            if (file != null) {
                writeDefaultTierConfig(file);
            }
        }

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static boolean loadTierConfig() {
        Map<AdvancedTier, TierSpec> previousSpecs = new EnumMap<>(TIER_SPECS);
        TIER_SPECS.clear();
        try {
            File file = resolveFile(tierConfigPath);
            if (file == null) {
                file = new File(configDirectory, "tiers.json");
            }
            JsonObject root = null;
            if (enableConfigurableTiers) {
                if (!file.isFile()) {
                    writeDefaultTierConfig(file);
                }
                try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    JsonElement parsed = new JsonParser().parse(reader);
                    if (!parsed.isJsonObject()) {
                        throw new IllegalArgumentException("The root must be a JSON object");
                    }
                    root = parsed.getAsJsonObject();
                } catch (Exception exception) {
                    throw new IllegalStateException("Unable to load tier configuration " + file, exception);
                }
            }
            if (root == null) {
                writeBuiltinTierSpecs();
                return true;
            }
            readTierConfig(root);
            return true;
        } catch (RuntimeException exception) {
            TIER_SPECS.clear();
            if (previousSpecs.isEmpty()) {
                writeBuiltinTierSpecs();
            } else {
                TIER_SPECS.putAll(previousSpecs);
            }
            if (com.jerry.mekextras.MekanismExtras.logger != null) {
                com.jerry.mekextras.MekanismExtras.logger.error(
                      "Invalid tiers configuration; keeping the last valid definitions", exception);
            } else {
                throw exception;
            }
            return false;
        }
    }

    public static void refreshDefaultTierConfig() {
        File file = getTierConfigFile();
        if (file == null) return;
        writeDefaultTierConfig(file);
        enableConfigurableTiers = true;
        tierConfigPath = file.getPath();
        if (config != null) {
            config.get("tiers", "enableConfigurableTiers", true,
                  "Read tier definitions from tiers.json. Disabled configurations fall back to the built-in defaults.")
                  .set(true);
            config.get("tiers", "tierConfigPath", file.getPath(),
                  "Path relative to config/mekanism_extras, or an absolute path.").set(file.getPath());
            config.save();
        }
        reloadTierConfig();
    }

    public static File getTierConfigFile() {
        File file = resolveFile(tierConfigPath);
        return file == null ? configDirectory == null ? null : new File(configDirectory, "tiers.json") : file;
    }

    public static void reloadTierConfigAfterConfigChange() {
        if (reloadTiersOnConfigChange) {
            reloadTierConfig();
        }
    }

    public static boolean reloadTierConfig() {
        boolean loaded = loadTierConfig();
        if (loaded) {
            generateResourcePack();
        }
        return loaded;
    }

    public static void generateResourcePack() {
        File tierFile = getTierConfigFile();
        if (tierFile == null) return;
        if (!tierFile.isFile()) {
            if (enableConfigurableTiers) {
                writeDefaultTierConfig(tierFile);
            } else {
                return;
            }
        }
        try {
            com.jerry.mekextras.common.config.ExtraTierResourceGenerator.generate(tierFile, getConfigDirectory());
        } catch (Exception exception) {
            if (com.jerry.mekextras.MekanismExtras.logger != null) {
                com.jerry.mekextras.MekanismExtras.logger.error("Unable to generate tier resource pack", exception);
            }
        }
    }

    public static Map<String, Integer> getTierIndexes() {
        Map<String, Integer> indexes = new LinkedHashMap<>();
        for (AdvancedTier tier : AdvancedTier.values()) {
            indexes.put(getTierId(tier, tier.name().toLowerCase(Locale.ROOT)), tier.ordinal());
        }
        return indexes;
    }

    public static float[] getDisplayRotation(AdvancedTier tier, String perspective, float[] fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null) return fallback;
        String value = spec.textures.get("display." + perspective + ".rotation");
        return value == null || value.isEmpty() ? fallback : parseVector(value, fallback);
    }

    public static float[] getDisplayTranslation(AdvancedTier tier, String perspective, float[] fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null) return fallback;
        String value = spec.textures.get("display." + perspective + ".translation");
        return value == null || value.isEmpty() ? fallback : parseVector(value, fallback);
    }

    public static float[] getDisplayScale(AdvancedTier tier, String perspective, float[] fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null) return fallback;
        String value = spec.textures.get("display." + perspective + ".scale");
        return value == null || value.isEmpty() ? fallback : parseVector(value, fallback);
    }

    private static float[] parseVector(String value, float[] fallback) {
        String[] parts = value.trim().split("[\\s,;]+");
        if (parts.length != 3) return fallback;
        try {
            return new float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2])};
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Nullable
    private static File resolveFile(String configuredPath) {
        if (configDirectory == null || configuredPath == null || configuredPath.trim().isEmpty()) {
            return null;
        }
        File file = new File(configuredPath.trim());
        return file.isAbsolute() ? file : new File(configDirectory, configuredPath.trim());
    }

    private static void readTierConfig(JsonObject root) {
        JsonArray array = getArray(root, "tiers");
        if (array == null || array.size() == 0) {
            throw new IllegalArgumentException("tiers.json must contain a non-empty tiers array");
        }
        for (int i = 0; i < array.size(); i++) {
            JsonObject item = asObject(array.get(i), "tiers[" + i + "]");
            String id = getString(item, "id", null);
            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException("tiers[" + i + "].id is required");
            }
            AdvancedTier tier = byId(id);
            if (tier == null) {
                throw new IllegalArgumentException("Unknown tier id: " + id);
            }
            if (TIER_SPECS.containsKey(tier)) {
                throw new IllegalArgumentException("Duplicate tier id: " + id);
            }
            TierSpec spec = new TierSpec(tier);
            spec.id = id.trim().toLowerCase(Locale.ROOT);
            spec.name = getString(item, "displayName", tier.name());
            spec.localizationKey = getString(item, "localizationKey", "tier.mekanism_extras." + spec.id);
            JsonObject localizations = getObject(item, "localizations");
            if (localizations != null) {
                for (Map.Entry<String, JsonElement> entry : localizations.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        spec.localizations.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().getAsString());
                    }
                }
            }
            if (localizations == null || !localizations.has("zh_cn")) {
                throw new IllegalArgumentException("localizations.zh_cn is required for tier " + spec.id);
            }
            if (spec.localizations.get("zh_cn").trim().isEmpty()) {
                throw new IllegalArgumentException("localizations.zh_cn must not be empty for tier " + spec.id);
            }
            spec.rgb = parseRgb(getString(item, "color", getString(item, "rgb", null)), tier.getRgb());
            spec.animated = getBoolean(item, "animated", false);
            spec.frameTicks = getFloat(item, "frameTicks", 7.0F);
            if (!(spec.frameTicks > 0)) {
                throw new IllegalArgumentException("frameTicks for tier " + spec.id + " must be greater than zero");
            }
            JsonArray colors = getArray(item, "palette");
            if (colors != null) {
                for (JsonElement color : colors) {
                    spec.palette.add(parseRgb(color.getAsString(), spec.rgb));
                }
            }
            JsonArray paletteOrder = getArray(item, "paletteOrder");
            if (paletteOrder != null) {
                for (JsonElement index : paletteOrder) {
                    int value = index.getAsInt();
                    if (value < 0 || value >= spec.palette.size()) {
                        throw new IllegalArgumentException("Invalid paletteOrder index " + value + " for tier " + spec.id);
                    }
                    spec.paletteOrder.add(value);
                }
            }
            if (spec.paletteOrder.isEmpty() && !spec.palette.isEmpty()) {
                for (int index = 0; index < spec.palette.size(); index++) {
                    spec.paletteOrder.add(index);
                }
            }
            if (spec.animated && !spec.palette.isEmpty() && spec.paletteOrder.size() < 2) {
                throw new IllegalArgumentException("Animated tier " + spec.id + " requires at least two paletteOrder entries");
            }
            spec.texturePrefix = getString(item, "texturePrefix", spec.id);
            spec.modelPrefix = getString(item, "modelPrefix", spec.id);
            spec.defaultModel = getString(item, "defaultModel", null);
            spec.machineTexture = getString(item, "machineTexture", "mekanism_extras:blocks/models/" + spec.texturePrefix + "/machine");
            spec.activeMachineTexture = getString(item, "activeMachineTexture", spec.machineTexture);
            spec.ledTexture = getString(item, "ledTexture", null);
            spec.energyCubeTexture = getString(item, "energyCubeTexture", "mekanism:blocks/models/energy_cube");
            spec.fluidTankTexture = getString(item, "fluidTankTexture", "mekanism:blocks/models/fluid_tank");
            spec.wireTexture = getString(item, "wireTexture", null);
            String defaultPrevious = tier == AdvancedTier.ABSOLUTE ? null : previous(spec.id);
            spec.installedFrom = getString(item, "installedFrom", defaultPrevious);
            if (spec.installedFrom != null && !spec.installedFrom.isEmpty()
                  && !spec.installedFrom.equalsIgnoreCase("upstream ultimate")
                  && findTierById(spec.installedFrom) == null) {
                throw new IllegalArgumentException("Unknown installedFrom tier for " + spec.id + ": " + spec.installedFrom);
            }
            spec.obtainable = getBoolean(item, "obtainable", true);
            spec.configurableBlockModels = getBoolean(item, "configurableBlockModels", false);
            spec.configurableItemModels = getBoolean(item, "configurableItemModels", true);
            spec.installerItem = qualifyItemId(getString(item, "installerItem", spec.id + "_tier_installer"));
            spec.controlCircuitItem = qualifyItemId(getString(item, "controlCircuitItem", spec.id + "_control_circuit"));
            spec.alloyItem = getString(item, "alloyItem", null);
            spec.alloyOreDictionary = getString(item, "alloyOreDictionary", null);
            JsonObject blocks = getObject(item, "blocks");
            if (blocks != null) {
                for (Map.Entry<String, JsonElement> entry : blocks.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        spec.blocks.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            JsonObject textures = getObject(item, "textures");
            if (textures != null) {
                for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        spec.textures.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            JsonObject display = getObject(item, "display");
            if (display != null) {
                for (Map.Entry<String, JsonElement> entry : display.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        spec.textures.put("display." + entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            JsonObject models = getObject(item, "models");
            if (models != null) {
                for (Map.Entry<String, JsonElement> entry : models.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        spec.models.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            JsonObject itemTextures = getObject(item, "itemTextures");
            if (itemTextures != null) {
                for (Map.Entry<String, JsonElement> entry : itemTextures.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        spec.itemTextures.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
            JsonObject values = getObject(item, "values");
            if (values != null) {
                for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        double value = entry.getValue().getAsDouble();
                        if (!Double.isFinite(value)) {
                            throw new IllegalArgumentException("values." + entry.getKey() + " must be finite");
                        }
                        if (value < 0) {
                            throw new IllegalArgumentException("values." + entry.getKey() + " must not be negative");
                        }
                        if (entry.getKey().contains("Processes") && (value < 1 || value != Math.rint(value))) {
                            throw new IllegalArgumentException("values." + entry.getKey()
                                  + " must be a positive integer");
                        }
                        spec.values.put(entry.getKey(), value);
                    }
                }
            }
            TIER_SPECS.put(tier, spec);
        }
        // Older four-tier configurations remain valid after adding Luna.
        if (!TIER_SPECS.containsKey(AdvancedTier.LUNA)) {
            TierSpec luna = defaultLunaSpec();
            for (AdvancedTier candidate : AdvancedTier.values()) {
                if (candidate == AdvancedTier.LUNA || !TIER_SPECS.containsKey(candidate)) continue;
                boolean hasSuccessor = false;
                for (TierSpec spec : TIER_SPECS.values()) {
                    if (spec.installedFromMatches(candidate)) {
                        hasSuccessor = true;
                        break;
                    }
                }
                if (!hasSuccessor) {
                    luna.installedFrom = candidate.name().toLowerCase(Locale.ROOT);
                    break;
                }
            }
            TIER_SPECS.put(AdvancedTier.LUNA, luna);
        }
        for (AdvancedTier tier : AdvancedTier.values()) {
            if (!TIER_SPECS.containsKey(tier)) {
                throw new IllegalArgumentException("Missing required tier definition: " + tier.name());
            }
        }
        validateTierChain();
    }

    private static void validateTierChain() {
        AdvancedTier startingTier = byId("absolute");
        if (startingTier == null) {
            throw new IllegalArgumentException("The absolute tier definition is required");
        }
        TierSpec absolute = TIER_SPECS.get(startingTier);
        if (absolute == null) {
            throw new IllegalArgumentException("The absolute tier definition is required");
        }
        if (absolute.installedFrom != null && !absolute.installedFrom.isEmpty()
              && !absolute.installedFrom.equalsIgnoreCase("upstream ultimate")) {
            throw new IllegalArgumentException("absolute.installedFrom must be upstream ultimate or empty");
        }
        java.util.Set<AdvancedTier> visited = new java.util.HashSet<>();
        AdvancedTier current = startingTier;
        while (current != null) {
            if (!visited.add(current)) {
                throw new IllegalArgumentException("Circular installedFrom chain at tier " + ExtraConfig.getTierId(current, "?"));
            }
            AdvancedTier next = null;
            for (AdvancedTier candidate : AdvancedTier.values()) {
                TierSpec spec = TIER_SPECS.get(candidate);
                if (spec != null && spec.installedFromMatches(current)) {
                    if (next != null) {
                        throw new IllegalArgumentException("Multiple tiers install from " + current.name());
                    }
                    next = candidate;
                }
            }
            current = next;
        }
        if (visited.size() != AdvancedTier.values().length) {
            throw new IllegalArgumentException("installedFrom must form one complete chain through every built-in tier");
        }
    }

    @Nullable
    private static AdvancedTier byId(String id) {
        for (AdvancedTier tier : AdvancedTier.values()) {
            if (tier.name().equalsIgnoreCase(id) || tier.getLowerName().equalsIgnoreCase(id)
                  || tier.getSimpleName().equalsIgnoreCase(id)) {
                return tier;
            }
        }
        return null;
    }

    private static void writeBuiltinTierSpecs() {
        TIER_SPECS.put(AdvancedTier.ABSOLUTE, new TierSpec(AdvancedTier.ABSOLUTE, "absolute", "Absolute", 0xE8E983));
        TIER_SPECS.put(AdvancedTier.SUPREME, new TierSpec(AdvancedTier.SUPREME, "supreme", "Supreme", 0xE65249));
        TierSpec cosmic = new TierSpec(AdvancedTier.COSMIC, "cosmic", "Cosmic", 0xFACEF7);
        cosmic.palette.addAll(Arrays.asList(0xF516EF, 0xF761F2, 0xF99DF5, 0xFAC0F6, 0xFACEF7));
        cosmic.frameTicks = 6.0F;
        cosmic.animated = true;
        TIER_SPECS.put(AdvancedTier.COSMIC, cosmic);
        TierSpec infinite = new TierSpec(AdvancedTier.INFINITE, "infinite", "Infinite", 0xDFD9AB);
        infinite.palette.addAll(Arrays.asList(0xDCC0AB, 0xD6A1B8, 0xCF9FD5, 0xA9B0DC, 0xA9D5C7, 0xDFD9AB));
        infinite.frameTicks = 7.0F;
        infinite.animated = true;
        TIER_SPECS.put(AdvancedTier.INFINITE, infinite);
        TIER_SPECS.put(AdvancedTier.LUNA, defaultLunaSpec());
    }

    private static TierSpec defaultLunaSpec() {
        TierSpec luna = new TierSpec(AdvancedTier.LUNA, "luna", "Luna", 0xFFFFFF);
        luna.installedFrom = "infinite";
        luna.alloyItem = "mekanism_extras:alloy_luna";
        luna.localizations.put("en_us", "Luna");
        luna.localizations.put("zh_cn", "Luna");
        return luna;
    }

    private static void writeDefaultTierConfig(File file) {
        JsonObject root = getTemplate();
        root.addProperty("description", "Mekanism Extras Legacy tier definitions. Built-in tier IDs are absolute, supreme, cosmic, infinite and luna.");
        try {
            Files.createDirectories(file.getParentFile().toPath());
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to write default tier configuration " + file, exception);
        }
    }

    private static JsonObject lunaTierJson() {
        JsonObject tier = new JsonObject();
        tier.addProperty("id", "luna");
        tier.addProperty("displayName", "Luna");
        tier.addProperty("localizationKey", "tier.mekanism_extras.luna");
        JsonObject localizations = new JsonObject();
        localizations.addProperty("en_us", "Luna");
        localizations.addProperty("zh_cn", "Luna");
        tier.add("localizations", localizations);
        tier.addProperty("color", "ffffff");
        tier.addProperty("animated", false);
        tier.addProperty("obtainable", true);
        tier.addProperty("installedFrom", "infinite");
        tier.addProperty("installerItem", "mekanism_extras:luna_tier_installer");
        tier.addProperty("controlCircuitItem", "mekanism_extras:luna_control_circuit");
        tier.addProperty("alloyItem", "mekanism_extras:alloy_luna");
        tier.addProperty("alloyOreDictionary", "alloyLuna");
        JsonObject values = new JsonObject();
        values.addProperty("extraFactoryProcesses", 21);
        values.addProperty("extraMachineProcesses", 21);
        values.addProperty("cableCapacity", 1000000000);
        values.addProperty("mechanicalPipeCapacity", 65536000);
        values.addProperty("mechanicalPipePull", 6553600);
        values.addProperty("pressurizedTubeCapacity", 16777216);
        values.addProperty("pressurizedTubePull", 4194304);
        values.addProperty("transporterSpeed", 70);
        values.addProperty("transporterPull", 64);
        values.addProperty("conductorInverseConduction", 5);
        values.addProperty("conductorHeatCapacity", 2560);
        values.addProperty("conductorInsulation", 312500000);
        tier.add("values", values);
        return tier;
    }

    private static JsonObject tierJson(String id, String displayName, String color, boolean animated,
                                       @Nullable String[] palette, String texturePrefix, String modelPrefix,
                                       String installer, String circuit, String alloy) {
        JsonObject tier = new JsonObject();
        tier.addProperty("id", id);
        tier.addProperty("displayName", displayName);
        tier.addProperty("localizationKey", "tier.mekanism_extras." + id);
        JsonObject localizations = new JsonObject();
        localizations.addProperty("en_us", displayName);
        switch (id) {
            case "absolute": localizations.addProperty("zh_cn", "绝对"); break;
            case "supreme": localizations.addProperty("zh_cn", "至尊"); break;
            case "cosmic": localizations.addProperty("zh_cn", "寰宇支配"); break;
            case "infinite": localizations.addProperty("zh_cn", "悖论无限"); break;
            default: break;
        }
        tier.add("localizations", localizations);
        tier.addProperty("color", color);
        tier.addProperty("animated", animated);
        tier.addProperty("obtainable", true);
        if (palette != null) {
            JsonArray values = new JsonArray();
            for (String value : palette) {
                values.add(value);
            }
            tier.add("palette", values);
        }
        tier.addProperty("texturePrefix", texturePrefix);
        tier.addProperty("modelPrefix", modelPrefix);
        tier.addProperty("machineTexture", "mekanism_extras:blocks/models/" + texturePrefix + "/machine");
        tier.addProperty("activeMachineTexture", "mekanism_extras:blocks/models/" + texturePrefix + "/machine_active");
        tier.addProperty("ledTexture", "mekanism_extras:blocks/factory/" + (id.equals("absolute") ? "led_absolute" : "led_" + id));
        tier.addProperty("energyCubeTexture", "mekanism:blocks/models/energy_cube");
        tier.addProperty("fluidTankTexture", "mekanism:blocks/models/fluid_tank");
        tier.addProperty("wireTexture", "mekanism:blocks/models/transmitter");
        tier.addProperty("configurableBlockModels", false);
        tier.addProperty("configurableItemModels", true);
        tier.addProperty("installedFrom", id.equals("absolute") ? "upstream ultimate" : previous(id));
        JsonObject values = new JsonObject();
        switch (id) {
            case "absolute":
                values.addProperty("cableCapacity", 1600000);
                values.addProperty("mechanicalPipeCapacity", 256000);
                values.addProperty("mechanicalPipePull", 25600);
                values.addProperty("pressurizedTubeCapacity", 65536);
                values.addProperty("pressurizedTubePull", 16384);
                values.addProperty("transporterSpeed", 30);
                values.addProperty("transporterPull", 4);
                values.addProperty("conductorInverseConduction", 5);
                values.addProperty("conductorHeatCapacity", 10);
                values.addProperty("conductorInsulation", 500000);
                values.addProperty("extraFactoryProcesses", 11);
                values.addProperty("extraMachineProcesses", 11);
                values.addProperty("energyCubeCapacity", 1024000000L);
                values.addProperty("energyCubeOutput", 1024000);
                values.addProperty("fluidTankCapacity", 4096000);
                values.addProperty("fluidTankOutput", 2048000);
                values.addProperty("chemicalTankCapacity", 131072000L);
                values.addProperty("chemicalTankOutput", 65536000L);
                break;
            case "supreme":
                values.addProperty("cableCapacity", 8000000);
                values.addProperty("mechanicalPipeCapacity", 1024000);
                values.addProperty("mechanicalPipePull", 102400);
                values.addProperty("pressurizedTubeCapacity", 262144);
                values.addProperty("pressurizedTubePull", 65536);
                values.addProperty("transporterSpeed", 40);
                values.addProperty("transporterPull", 8);
                values.addProperty("conductorInverseConduction", 5);
                values.addProperty("conductorHeatCapacity", 40);
                values.addProperty("conductorInsulation", 2500000);
                values.addProperty("extraFactoryProcesses", 13);
                values.addProperty("extraMachineProcesses", 13);
                values.addProperty("energyCubeCapacity", 4096000000L);
                values.addProperty("energyCubeOutput", 4096000);
                values.addProperty("fluidTankCapacity", 32768000);
                values.addProperty("fluidTankOutput", 16384000);
                values.addProperty("chemicalTankCapacity", 4194304000L);
                values.addProperty("chemicalTankOutput", 2097150000L);
                break;
            case "cosmic":
                values.addProperty("cableCapacity", 40000000);
                values.addProperty("mechanicalPipeCapacity", 4096000);
                values.addProperty("mechanicalPipePull", 409600);
                values.addProperty("pressurizedTubeCapacity", 1048576);
                values.addProperty("pressurizedTubePull", 262144);
                values.addProperty("transporterSpeed", 50);
                values.addProperty("transporterPull", 16);
                values.addProperty("conductorInverseConduction", 5);
                values.addProperty("conductorHeatCapacity", 160);
                values.addProperty("conductorInsulation", 12500000);
                values.addProperty("extraFactoryProcesses", 15);
                values.addProperty("extraMachineProcesses", 15);
                values.addProperty("energyCubeCapacity", 16384000000L);
                values.addProperty("energyCubeOutput", 16384000);
                values.addProperty("fluidTankCapacity", 262144000);
                values.addProperty("fluidTankOutput", 131072000);
                values.addProperty("chemicalTankCapacity", 268435456000L);
                values.addProperty("chemicalTankOutput", 134217728000L);
                break;
            case "infinite":
                values.addProperty("cableCapacity", 200000000);
                values.addProperty("mechanicalPipeCapacity", 16384000);
                values.addProperty("mechanicalPipePull", 1638400);
                values.addProperty("pressurizedTubeCapacity", 4194304);
                values.addProperty("pressurizedTubePull", 1048576);
                values.addProperty("transporterSpeed", 60);
                values.addProperty("transporterPull", 32);
                values.addProperty("conductorInverseConduction", 5);
                values.addProperty("conductorHeatCapacity", 640);
                values.addProperty("conductorInsulation", 62500000);
                values.addProperty("extraFactoryProcesses", 17);
                values.addProperty("extraMachineProcesses", 17);
                values.addProperty("energyCubeCapacity", 65536000000L);
                values.addProperty("energyCubeOutput", 65536000);
                values.addProperty("fluidTankCapacity", 2097152000);
                values.addProperty("fluidTankOutput", 1048576000);
                values.addProperty("chemicalTankCapacity", 34359738368000L);
                values.addProperty("chemicalTankOutput", 17179869184000L);
                break;
            default:
                break;
        }
        tier.add("values", values);
        tier.addProperty("installerItem", installer);
        tier.addProperty("controlCircuitItem", circuit);
        tier.addProperty("alloyItem", alloy);
        tier.addProperty("alloyOreDictionary", "alloy" + Character.toUpperCase(id.charAt(0)) + id.substring(1));
        JsonObject blocks = new JsonObject();
        for (String suffix : new String[]{"bin", "energy_cube", "fluid_tank", "chemical_tank", "radioactive_waste_barrel"}) {
            blocks.addProperty(suffix, "mekanism_extras:" + id + "_" + suffix);
        }
        tier.add("blocks", blocks);
        tier.addProperty("resourcePack", "");
        JsonObject itemTextures = new JsonObject();
        itemTextures.addProperty("installer", "mekanism_extras:items/" + id + "_tier_installer");
        itemTextures.addProperty("circuit", "mekanism_extras:items/" + id + "_control_circuit");
        itemTextures.addProperty("alloy", "mekanism_extras:items/" + alloy.substring("mekanism_extras:".length()));
        tier.add("itemTextures", itemTextures);
        JsonObject models = new JsonObject();
        models.addProperty("machine", "");
        models.addProperty("energy_cube", "mekanism:energy_cube");
        models.addProperty("fluid_tank", "mekanism:fluid_tank");
        models.addProperty("wire", "mekanism_extras:block/{tier}_universal_cable");
        tier.add("models", models);
        JsonObject display = new JsonObject();
        display.addProperty("thirdperson_righthand.rotation", "75 45 0");
        display.addProperty("thirdperson_righthand.translation", "0 2.5 0");
        display.addProperty("thirdperson_righthand.scale", "0.375 0.375 0.375");
        display.addProperty("thirdperson_lefthand.rotation", "75 45 0");
        display.addProperty("thirdperson_lefthand.translation", "0 2.5 0");
        display.addProperty("thirdperson_lefthand.scale", "0.375 0.375 0.375");
        display.addProperty("firstperson_righthand.rotation", "0 45 0");
        display.addProperty("firstperson_righthand.translation", "0 0 0");
        display.addProperty("firstperson_righthand.scale", "0.4 0.4 0.4");
        display.addProperty("firstperson_lefthand.rotation", "0 225 0");
        display.addProperty("firstperson_lefthand.translation", "0 0 0");
        display.addProperty("firstperson_lefthand.scale", "0.4 0.4 0.4");
        display.addProperty("gui.rotation", "30 225 0");
        display.addProperty("gui.translation", "0 0 0");
        display.addProperty("gui.scale", "0.625 0.625 0.625");
        display.addProperty("ground.rotation", "0 0 0");
        display.addProperty("ground.translation", "0 2 0");
        display.addProperty("ground.scale", "0.25 0.25 0.25");
        tier.add("display", display);
        return tier;
    }

    private static String previous(String id) {
        switch (id) {
            case "supreme": return "absolute";
            case "cosmic": return "supreme";
            case "infinite": return "cosmic";
            case "luna": return "infinite";
            default: return "";
        }
    }

    public static String getTierId(AdvancedTier tier, String fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? fallback : spec.id;
    }

    public static boolean hasConfigurableBlockModels(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec != null && spec.configurableBlockModels;
    }

    public static boolean hasConfigurableItemModels(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec != null && spec.configurableItemModels;
    }

    public static boolean isNextTier(AdvancedTier current, AdvancedTier target) {
        if (current == null || target == null) return false;
        TierSpec spec = TIER_SPECS.get(target);
        if (spec != null && spec.installedFrom != null && !spec.installedFrom.isEmpty()) {
            return spec.installedFrom.equalsIgnoreCase(current.name())
                  || spec.installedFrom.equalsIgnoreCase(current.getLowerName())
                  || spec.installedFrom.equalsIgnoreCase(current.getSimpleName());
        }
        return target.ordinal() == current.ordinal() + 1;
    }

    public static AdvancedTier getPreviousTier(AdvancedTier tier) {
        if (tier == null || tier.ordinal() == 0) return null;
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec != null && spec.installedFrom != null && !spec.installedFrom.isEmpty()) {
            for (AdvancedTier candidate : AdvancedTier.values()) {
                if (spec.installedFrom.equalsIgnoreCase(candidate.name())
                      || spec.installedFrom.equalsIgnoreCase(candidate.getLowerName())
                      || spec.installedFrom.equalsIgnoreCase(candidate.getSimpleName())) {
                    return candidate;
                }
            }
        }
        return AdvancedTier.byIndex(tier.ordinal() - 1);
    }

    public static AdvancedTier getNextTier(AdvancedTier tier) {
        if (tier == null) return null;
        for (AdvancedTier candidate : AdvancedTier.values()) {
            TierSpec spec = TIER_SPECS.get(candidate);
            if (spec != null && spec.installedFrom != null && !spec.installedFrom.isEmpty()) {
                if (spec.installedFrom.equalsIgnoreCase(tier.name())
                      || spec.installedFrom.equalsIgnoreCase(tier.getLowerName())
                      || spec.installedFrom.equalsIgnoreCase(tier.getSimpleName())) {
                    return candidate;
                }
            } else if (candidate.ordinal() == tier.ordinal() + 1) {
                return candidate;
            }
        }
        return null;
    }

    public static String getTierName(AdvancedTier tier, String fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? fallback : spec.name;
    }

    public static int getTierRgb(AdvancedTier tier, int fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? fallback : spec.rgb;
    }

    public static List<Integer> getTierPalette(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null || spec.palette.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(spec.palette);
    }

    public static List<Integer> getTierPaletteOrder(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null || spec.paletteOrder.isEmpty()
              ? Collections.emptyList() : Collections.unmodifiableList(spec.paletteOrder);
    }

    public static float getTierFrameTicks(AdvancedTier tier, float fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null || spec.frameTicks <= 0 ? fallback : spec.frameTicks;
    }

    public static EnumColor getTierColor(AdvancedTier tier, EnumColor fallback) {
        return fallback;
    }

    public static boolean isTierObtainable(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null || spec.obtainable;
    }

    public static String getLocalizedTierName(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null) return null;
        String language = getLanguage();
        String value = spec.localizations.get(language);
        if (value == null) value = spec.localizations.get("en_us");
        return value;
    }

    private static String getLanguage() {
        try {
            return net.minecraft.client.Minecraft.getMinecraft().gameSettings.language.toLowerCase(Locale.ROOT);
        } catch (LinkageError | RuntimeException ignored) {
            return "en_us";
        }
    }

    public static String getInstallerItem(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? "mekanism_extras:" + tier.getLowerName() + "_tier_installer" : spec.installerItem;
    }

    public static String getControlCircuitItem(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? "mekanism_extras:" + tier.getLowerName() + "_control_circuit" : spec.controlCircuitItem;
    }

    @Nullable
    public static String getItemTexture(AdvancedTier tier, String kind) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? null : emptyToNull(spec.itemTextures.get(kind));
    }

    public static int getInt(AdvancedTier tier, String key, int fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null || !spec.values.containsKey(key)) return fallback;
        return (int) Math.round(spec.values.get(key));
    }

    public static double getDouble(AdvancedTier tier, String key, double fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null || !spec.values.containsKey(key)) return fallback;
        return spec.values.get(key);
    }

    public static long getLong(AdvancedTier tier, String key, long fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null || !spec.values.containsKey(key)) return fallback;
        return Math.round(spec.values.get(key));
    }

    @Nullable
    public static String getAlloyItem(AdvancedTier tier) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? null : spec.alloyItem;
    }

    public static String getAlloyItemId(AdvancedTier tier, String fallback) {
        String configured = getAlloyItem(tier);
        if (configured == null || configured.isEmpty()) {
            return fallback;
        }
        return stripNamespace(configured);
    }

    public static boolean isInstallerFor(AdvancedTier target, @Nullable String itemId) {
        if (itemId == null) return false;
        String configured = getInstallerItem(target);
        if (configured.equals(itemId)) return true;
        return stripNamespace(configured).equals(stripNamespace(itemId));
    }

    public static boolean isAlloyFor(AdvancedTier target, @Nullable String itemId) {
        if (itemId == null) return false;
        String configured = getAlloyItem(target);
        if (configured == null || configured.isEmpty()) return false;
        return configured.equals(itemId) || stripNamespace(configured).equals(stripNamespace(itemId));
    }

    @Nullable
    public static AdvancedTier getTargetForAlloy(@Nullable String itemId, AdvancedTier fallback) {
        for (AdvancedTier tier : AdvancedTier.values()) {
            if (isAlloyFor(tier, itemId)) {
                return tier;
            }
        }
        return fallback;
    }

    public static String getTexture(AdvancedTier tier, String key, String fallback) {
        TierSpec spec = TIER_SPECS.get(tier);
        if (spec == null) return fallback;
        String configured = spec.textures.get(key);
        if (configured != null && !configured.isEmpty()) return configured;
        switch (key) {
            case "machine": return emptyTo(spec.machineTexture, fallback);
            case "machine_active": return emptyTo(spec.activeMachineTexture, fallback);
            case "led": return emptyTo(spec.ledTexture, fallback);
            case "energy_cube": return emptyTo(spec.energyCubeTexture, fallback);
            case "fluid_tank": return emptyTo(spec.fluidTankTexture, fallback);
            case "wire": return emptyTo(spec.wireTexture, fallback);
            default: return fallback;
        }
    }

    @Nullable
    public static String getModelPattern(AdvancedTier tier, String key) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? null : emptyToNull(spec.models.get(key));
    }

    @Nullable
    public static String getBlockId(AdvancedTier tier, String type) {
        TierSpec spec = TIER_SPECS.get(tier);
        return spec == null ? null : emptyToNull(spec.blocks.get(type));
    }

    private static String emptyTo(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static String qualifyItemId(String value) {
        return value != null && value.indexOf(':') < 0 ? "mekanism_extras:" + value : value;
    }

    private static String stripNamespace(String value) {
        int separator = value.indexOf(':');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    private static boolean matchesTierId(@Nullable String configured, AdvancedTier tier) {
        return configured != null && !configured.isEmpty()
              && (configured.equalsIgnoreCase(tier.name())
              || configured.equalsIgnoreCase(tier.getLowerName())
              || configured.equalsIgnoreCase(tier.getSimpleName()));
    }

    @Nullable
    private static AdvancedTier findTierById(String id) {
        for (AdvancedTier tier : AdvancedTier.values()) {
            if (matchesTierId(id, tier)) {
                return tier;
            }
        }
        return null;
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static int parseRgb(@Nullable String value, int fallback) {
        if (value == null || value.isEmpty()) return fallback;
        String clean = value.trim().replace("#", "").replace("0x", "");
        if (clean.length() == 6) clean = "ff" + clean;
        try {
            return (int) Long.parseLong(clean, 16) & 0xFFFFFF;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid RGB color: " + value, exception);
        }
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    private static JsonArray intArray(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) {
            array.add(value);
        }
        return array;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static JsonObject asObject(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(path + " must be an object");
        }
        return element.getAsJsonObject();
    }

    @Nullable
    private static String getString(JsonObject object, String key, @Nullable String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    private static float getFloat(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsFloat();
    }

    private static class TierSpec {
        final AdvancedTier tier;
        String id;
        String name;
        String localizationKey;
        int rgb;
        boolean animated;
        float frameTicks;
        final List<Integer> palette = new ArrayList<>();
        final List<Integer> paletteOrder = new ArrayList<>();
        String texturePrefix;
        String modelPrefix;
        String defaultModel;
        String machineTexture;
        String activeMachineTexture;
        String ledTexture;
        String energyCubeTexture;
        String fluidTankTexture;
        String wireTexture;
        String installedFrom;
        boolean obtainable;
        boolean configurableBlockModels;
        boolean configurableItemModels;
        String installerItem;
        String controlCircuitItem;
        String alloyItem;
        String alloyOreDictionary;
        final Map<String, String> blocks = new LinkedHashMap<>();
        final Map<String, String> textures = new LinkedHashMap<>();
        final Map<String, String> models = new LinkedHashMap<>();
        final Map<String, String> itemTextures = new LinkedHashMap<>();
        final Map<String, String> localizations = new LinkedHashMap<>();
        final Map<String, Double> values = new LinkedHashMap<>();

        TierSpec(AdvancedTier tier) {
            this.tier = tier;
        }

        TierSpec(AdvancedTier tier, String id, String name, int rgb) {
            this.tier = tier;
            this.id = id;
            this.name = name;
            this.localizationKey = "tier.mekanism_extras." + id;
            this.rgb = rgb;
            this.texturePrefix = id;
            this.modelPrefix = id;
            this.installerItem = "mekanism_extras:" + id + "_tier_installer";
            this.controlCircuitItem = "mekanism_extras:" + id + "_control_circuit";
            this.obtainable = true;
        }

        boolean installedFromMatches(AdvancedTier candidate) {
            return matchesTierId(installedFrom, candidate);
        }
    }
}
