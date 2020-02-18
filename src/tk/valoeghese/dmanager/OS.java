package tk.valoeghese.dmanager;

import java.io.File;

public enum OS {
	WINDOWS; // only windows support currently

	public String getDiscordModulesLocation() {
		switch (this) {
		case WINDOWS:
			File discordMetaFolder = new File(System.getenv("APPDATA") + "/Discord");
			
			for (File f : discordMetaFolder.listFiles()) {
				if (f.isDirectory()) {
					if (f.getName().matches("\\d+(.\\d+(.\\d+)?)")) {
						return f.getPath() + "/modules/discord_modules";
					}
				}
			}
		}

		return null;
	}

	public static OS get() {
		String osstr = System.getProperty("os.name");

		if (osstr.startsWith("Windows")) {
			return WINDOWS;
		}

		return null;
	}
}
