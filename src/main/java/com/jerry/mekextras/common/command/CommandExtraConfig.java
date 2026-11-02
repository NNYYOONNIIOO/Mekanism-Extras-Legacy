package com.jerry.mekextras.common.command;

import com.jerry.mekextras.common.config.ExtraConfig;
import com.jerry.mekextras.common.config.ExtraTierResourceGenerator;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.server.command.CommandTreeBase;

import java.util.Map;

public class CommandExtraConfig extends CommandTreeBase {

    public CommandExtraConfig() {
        addSubcommand(new Reload());
        addSubcommand(new Template());
        addSubcommand(new Status());
    }

    @Override
    public String getName() {
        return "mekextrasconfig";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/mekextrasconfig <reload|template|status>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    private static class Reload extends CommandBase {
        @Override
        public String getName() {
            return "reload";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/mekextrasconfig reload";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
            if (ExtraConfig.reloadTierConfig()) {
                sender.sendMessage(new TextComponentString("Mekanism Extras 等级配置已重新载入；客户端材质可在资源重载后生效。"));
            } else {
                throw new CommandException("tiers.json 无效，已保留上一次有效配置；详情见 latest.log。");
            }
        }
    }

    private static class Template extends CommandBase {
        @Override
        public String getName() {
            return "template";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/mekextrasconfig template";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            ExtraConfig.refreshDefaultTierConfig();
            sender.sendMessage(new TextComponentString("已重新生成 config/mekanism_extras/tiers.json。"));
        }
    }

    private static class Status extends CommandBase {
        @Override
        public String getName() {
            return "status";
        }

        @Override
        public String getUsage(ICommandSender sender) {
            return "/mekextrasconfig status";
        }

        @Override
        public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
            Map<String, Integer> indexes = ExtraConfig.getTierIndexes();
            sender.sendMessage(new TextComponentString("Mekanism Extras 等级配置: " + indexes));
            sender.sendMessage(new TextComponentString("配置目录: " + ExtraConfig.getConfigDirectory()));
            sender.sendMessage(new TextComponentString("tiers.json: " + ExtraConfig.getTierConfigFile()));
            sender.sendMessage(new TextComponentString("资源包: " + ExtraTierResourceGenerator.getPackDirectory(
                  ExtraConfig.getConfigDirectory())));
        }
    }
}
