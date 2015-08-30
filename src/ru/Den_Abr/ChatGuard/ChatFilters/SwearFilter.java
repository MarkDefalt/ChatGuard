package ru.Den_Abr.ChatGuard.ChatFilters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import ru.Den_Abr.ChatGuard.ChatGuardPlugin;
import ru.Den_Abr.ChatGuard.Violation;
import ru.Den_Abr.ChatGuard.Configuration.Messages.Message;
import ru.Den_Abr.ChatGuard.Configuration.Settings;
import ru.Den_Abr.ChatGuard.Player.CGPlayer;

import com.google.common.io.Files;

public class SwearFilter extends AbstractFilter {
	private static List<Pattern> swearPatterns = new ArrayList<>();
	private String replacement;
	private boolean informAdmins;

	@Override
	public Violation checkMessage(String message, CGPlayer player) {
		if (player.hasPermission("chatguard.ignore.swear"))
			return null;
		Violation v = null;
		for (Pattern word : swearPatterns) {
			Matcher swearMatcher = word.matcher(message);
			if (swearMatcher.find()) {
				v = Violation.SWEAR;
				break;
			}
		}
		if (v != null && informAdmins) {
			informAdmins(player, message);
		}
		return v;
	}

	private void informAdmins(CGPlayer player, String message) {
		String complete = Message.INFORM_SWEAR.get()
				.replace("{PLAYER}", player.getName())
				.replace("{MESSAGE}", message);
		Bukkit.getConsoleSender().sendMessage(complete);
		Bukkit.broadcast(complete, "chatguard.inform.swear");
	}

	@Override
	public String getClearMessage(String message, CGPlayer player) {
		for (Pattern pattern : swearPatterns) {
			message = message.replaceAll(
					pattern.pattern(),
					Settings.isSeparatedWarnings() ? replacement : Settings
							.getReplacement());
		}
		return message;
	}

	@Override
	public void register() {
		ConfigurationSection cs = Settings.getConfig().getConfigurationSection(
				"swear settings");
		if (!cs.getBoolean("enabled"))
			return;
		informAdmins = cs.getBoolean("inform admins");
		maxWarns = cs.getInt("max warnings");
		replacement = ChatColor.translateAlternateColorCodes('&',
				cs.getString("custom replacement"));

		try {
			File oldFileSwear = new File(ChatGuardPlugin.getInstance()
					.getDataFolder(), "swearlist.txt");
			File newFileSwear = new File(ChatGuardPlugin.getInstance()
					.getDataFolder(), "swearwords.txt");
			if (!newFileSwear.exists()) {
				if (oldFileSwear.exists()) {
					String oldLine = Files.readFirstLine(oldFileSwear,
							Charset.forName("UTF-8")).replace('|', '\n');
					Files.write(oldLine.getBytes(Charset.forName("UTF-8")),
							newFileSwear);
					oldFileSwear
							.renameTo(new File(ChatGuardPlugin.getInstance()
									.getDataFolder(), "swearlistOLD.txt"));
					ChatGuardPlugin.getLog().info("Moved old swearlist file");
				} else {
					ChatGuardPlugin.getInstance().saveResource(
							"swearwords.txt", false);
					ChatGuardPlugin.getInstance().getLogger()
							.warning("Check your swearwords.txt file!");
				}
			}
			swearPatterns.clear();

			for (String word : new ArrayList<>(Files.readLines(newFileSwear,
					Charset.forName("UTF-8")))) {
				if (word.isEmpty())
					continue;
				swearPatterns.add(Pattern.compile(word,
						Pattern.CASE_INSENSITIVE));
			}
			getActiveFilters().add(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return;
	}

	public static void addWord(String w) {
		Pattern p = Pattern.compile(w);
		swearPatterns.add(p);
		File swearFile = new File(
				ChatGuardPlugin.getInstance().getDataFolder(), "swearwords.txt");
		try {
			Files.append("\n" + w, swearFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
