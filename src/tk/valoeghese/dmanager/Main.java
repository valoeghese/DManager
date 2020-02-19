package tk.valoeghese.dmanager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.zip.ZipFile;

public class Main {
	public static void main(String[] args) {
		OS os = OS.get();

		if (os == null) {
			System.err.println("Your Operating System is not currently supported!");
		}

		register("core.util", true, Optional.empty());
		register("core.event", true, Optional.empty(), "core.util");

		File pluginsFolder = new File("plugins/");

		if (pluginsFolder.mkdir()) { // if not created then there are already plugins installed
			return;
		} else {
			// load plugins
			for (File file : pluginsFolder.listFiles()) {
				if (!file.isDirectory() && file.getName().endsWith(".zip")) { // check is a zip (if a user tries to install a png image file called yeet.zip it's their fault)
					try {
						ZipFile zip = new ZipFile(file);
						loadResource(zip);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		}

		// check plugin dependencies are satisfied
		for (Module plugin : MODULE_LIST) {
			for (String dependency : plugin.dependencies) {
				if (!MODULES.containsKey(dependency)) {
					throw new RuntimeException("Dependency " + dependency + " is not satisfied for plugin " + plugin.id);
				}
			}
		}

		// Get default discord js file contents, via using backups of vanilla files (.vanilla)
		String discordModulesDir = os.getDiscordModulesLocation();
		File backupFile = new File(discordModulesDir + "/index.js.vanilla");
		File mainFile = new File(discordModulesDir + "/index.js");

		try {
			if (backupFile.createNewFile()) {
				try (FileOutputStream foss = new FileOutputStream(backupFile)) { // haha get it "FOSS" FileOutputStream (fos)
					try (FileInputStream arr = new FileInputStream(mainFile)) { // here, labeling my variables very well
						FileChannel source = arr.getChannel();                 // the words in that previous comment were in alphabetical order
						foss.getChannel().transferFrom(source, 0, source.size());
					}
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		String initial;

		try (FileInputStream fis = new FileInputStream(backupFile)) {
			byte[] data = new byte[(int) backupFile.length()];
			fis.read(data);
			initial = new String(data, "UTF-8");

			if (initial.isEmpty()) {
				throw new RuntimeException("read contents of vanilla discord_modules/index.js is empty! perhaps this is a bug in the program?");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// Proceed to install modules with a maximum of 256 passes to prevent infinite loop
		int remainingPasses = 255;
		List<Module> remaining = new ArrayList<>(MODULE_LIST);
		List<String> installed = new ArrayList<>();

		try (PrintWriter writer = new PrintWriter(mainFile)) {
			writer.print(initial);
			writer.println();

			if (!initial.endsWith("\n")) {
				writer.println();
			}
			writer.println("// DManager: Plugins Start ======================");
			writer.println();

			while (remainingPasses --> 0) {
				List<Integer> installedIndices = new ArrayList<>(); // array indexes of ones installed this session

				moduleLoop: for (int index = 0; index < remaining.size(); ++index) {
					Module m = remaining.get(index);

					for (String dependency : m.dependencies) {
						// check if dependencies are installed
						if (!installed.contains(dependency)) {
							continue moduleLoop; // does using labels count as "hacky code"
						}
					}

					// install module
					System.out.println("Installing " + m.id);
					writer.println("// DManager: plugin <" + m.id + ">");

					int depCount = m.dependencies.length;

					if (depCount > 1) {
						StringBuilder sb = new StringBuilder("// depends on ");

						for (String d : m.dependencies) {
							sb.append(d);
						}

						writer.println(sb.toString());
					} else if (depCount == 1) {
						writer.println("// depends on " + m.dependencies[0]);
					}

					// copy contents from plugin js file to the discord file

					try (BufferedReader reader = openBufferedReader(m.builtin ? m.file.openStream() : m.zipFile.getInputStream(m.zipFile.getEntry(m.id + ".js")))) {
						String lineIn;

						while ((lineIn = reader.readLine()) != null) {
							writer.println(lineIn);
						}
					} catch (IOException e) {
						alertCritial(e, writer);
					}

					// blank line between plugins
					writer.println();
					installed.add(m.id);
					installedIndices.add(index);
				}

				installedIndices.sort((a, b) -> b - a);
				for (Integer index : installedIndices) {
					remaining.remove(index.intValue());
				}

				if (remaining.size() == 0) {
					break;
				} else if (remainingPasses == 5) {
					System.out.println("It is taking quite a lot of tries to install all the plugins. Either you have a lot of plugins, The plugins are circular dependencies, or there is a bug in DManager causing this.");
				}
			}

			writer.println("// ====================== DManager: Plugins End");
		} catch (FileNotFoundException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static BufferedReader openBufferedReader(InputStream is) throws IOException {
		return new BufferedReader(new InputStreamReader(is));
	}

	private static void alertCritial(IOException e, PrintWriter writer) throws UncheckedIOException {
		writer.println("alert(\"DManager: There was a critial error in installing plugins!\nRemove all plugins and run DManager again to uninstall plugins.\nIf this error persists, manually locate and uninstall DManager from (Windows) %AppData%/Discord/(version)/modules/discord_modules/index.js\");");
		throw new UncheckedIOException(e);
	}

	private static void loadResource(ZipFile zip) throws IOException {
		try (InputStream is = zip.getInputStream(zip.getEntry("plugin.meta"))) {
			Scanner sc = new Scanner(is);
			String[] dependencies = new String[0];
			String resourceLocation = null;

			while (sc.hasNextLine()) {
				String[] lineData = sc.nextLine().trim().split("=");

				if (lineData.length >= 2 && !lineData[0].startsWith("#")) {
					switch (lineData[0].trim()) {
					case "dependencies":
						dependencies = lineData[1].trim().split(" ");
						break;
					case "id":
						resourceLocation = lineData[1].trim();
					}
				}
			}

			if (resourceLocation == null) {
				System.err.println("A plugin has no id! Skipping it!");
				sc.close();
				return;
			}

			register(resourceLocation, false, Optional.of(zip), dependencies);
			sc.close();
		}
	}

	private static void register(String resourceLocation, boolean dmanagerBuiltin, Optional<ZipFile> zipFile, String... dependencies) {
		Module module = new Module();

		if (dmanagerBuiltin) {
			module.file = Main.class.getClassLoader().getResource("tk/valoeghese/dmanager/resource/" + resourceLocation.replace('.', '/') + ".js");

			if (module.file == null) {
				throw new RuntimeException("Cannot load URL for plugin " + resourceLocation + "!");
			}
		}

		module.dependencies = dependencies;
		module.builtin = dmanagerBuiltin;
		module.id = resourceLocation;

		if (zipFile.isPresent()) {
			module.zipFile = zipFile.get();
		}

		MODULES.put(resourceLocation, module);
		MODULE_LIST.add(module);
	}

	private static final Map<String, Module> MODULES = new HashMap<>();
	private static final List<Module> MODULE_LIST = new ArrayList<>();
}

class Module {
	ZipFile zipFile; // external module only
	URL file; // internal module only
	String[] dependencies; // common
	boolean builtin; // common
	String id; // common
}