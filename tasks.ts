import { $ } from "bun";
import { platform, homedir } from "node:os";
import { cp, rm, mkdir, writeFile } from "node:fs/promises";
import { resolve, join, basename } from "node:path";
import { Glob } from "bun";

const CONFIG = {
  steam: {
    account: "userName",
    cmdWindows: "tools/steamContentBuilder/builder/steamcmd",
    cmdLinux: "./tools/steamContentBuilder/builder_linux/steamcmd.sh",
  },
  paths: {
    content: "tools/steamContentBuilder/content",
    libs: {
      base: "steamLibs",
      win: "steamLibs/windows64",
      linux: "steamLibs/linux64",
    },
    outputBase: "tools/game_builds",
    scriptsDir: "tools/steamContentBuilder/scripts",
  },
};

const log = {
  info: (msg: string) => console.log(`\x1b[34mℹ\x1b[0m ${msg}`),
  success: (msg: string) => console.log(`\x1b[32m✅ ${msg}\x1b[0m`),
  error: (msg: string) => console.error(`\x1b[31m❌ ${msg}\x1b[0m`),
  step: (msg: string) => console.log(`\x1b[36m🚀 [TASK] ${msg}\x1b[0m`),
};

function isWindows() {
  return platform() == "win32";
}

function getVdfPath(
  type: "preview" | "local" | "publish",
  suffixEnabled: bool,
): string {
  const suffix = isWindows() ? "windows" : "linux";
  return resolve(
    `${CONFIG.paths.scriptsDir}/app_build_${type}_${suffixEnabled ? suffix : "all"}.vdf`,
  );
}

async function ensureExecutable() {
  if (!isWindows()) {
    log.info("Checking permissions for steamcmd...");
    const cmdPath = CONFIG.steam.cmdLinux;
    const linux32Dir = resolve(
      "tools/steamContentBuilder/builder_linux/linux32/steamcmd",
    );
    await $`chmod +x ${cmdPath}`;
    await $`chmod +x ${linux32Dir}`.catch(() => {});
  }
}

async function performBuild(isRelease = false) {
  const isWin = isWindows();
  const task = isRelease
    ? "desktopApp:createReleaseDistributable"
    : "desktopApp:createDistributable";
  const subDir = isRelease ? "main-release" : "main";
  const buildFolder = isWin ? "build-win" : "build-linux";

  const gradleGenPath = `desktopApp/${buildFolder}/compose/binaries/${subDir}/app/GoodIdleGame`;
  const platformTag = isWin ? "windows-x64" : "linux-x64";
  const finalOutputPath = `${CONFIG.paths.outputBase}/${platformTag}/${isRelease ? "release" : "debug"}`;

  log.step(`Running Gradle task: ${task}`);
  await $`./gradlew ${task}`.throws(true);

  log.info(`Preparing directory: ${finalOutputPath}`);
  await rm(finalOutputPath, { recursive: true, force: true });
  await mkdir(finalOutputPath, { recursive: true });

  log.info(`Moving artifacts...`);
  await cp(gradleGenPath, finalOutputPath, { recursive: true });

  const libs = isWin
    ? [
        `${CONFIG.paths.libs.win}/steam_api64.dll`,
        `${CONFIG.paths.libs.win}/steamworks4j64.dll`,
      ]
    : [
        `${CONFIG.paths.libs.linux}/libsteam_api.so`,
        `${CONFIG.paths.libs.linux}/libsteamworks4j.so`,
      ];

  for (const lib of libs) {
    const fileName = basename(lib);
    const dest = join(finalOutputPath, fileName);

    await cp(lib, dest).catch(() => log.error(`Missing lib: ${lib}`));
  }

  if (!isRelease) {
    await cp(
      `${CONFIG.paths.libs.base}/steam_appid.txt`,
      join(finalOutputPath, "steam_appid.txt"),
    );
  }

  return finalOutputPath;
}

function getSteamCmdPath(): String {
  if (isWindows()) return CONFIG.steam.cmdWindows;
  return CONFIG.steam.cmdLinux;
}

async function translate_task(targetLang: string) {
  const potPath =
    "./sharedUI/src/commonMain/composeResources/files/i18n/messages.pot";
  const outputPath = `./sharedUI/src/commonMain/composeResources/files/i18n/messages_${targetLang}.po`;
  const endPoint = "endPoint";
  const apiKey = "key";
  const model = "model-name";
  const custom_prompt = `要翻译的文本是一款 Melvor Idle 风格的游戏，源语言是中英混杂的。
        ## 翻译准则
        1. **彻底本地化**：严禁在译文中保留任何非{目标语言}的字符（除非是数字或专有名词）。
           - 示例（日语）：使用「経験値」而非「经验」；使用「錬金」而非「炼金」。
        2. **标识符转换 (stat_id)**：若上下文为 stat_id，请将其转换为人类可读的属性名称。
           - 示例：actor_maxHealth -> {目标语言对应名称}
        3. **术语映射逻辑**：
           - 提供的术语库仅定义了**概念对应关系**。
           - **严禁**直接复制术语库中的中文或英文到{目标语言}的译文中。
           - 你必须根据术语库中的概念，将其翻译为地道的{目标语言}。

        ## 术语概念定义 (概念参考)
        - **Formater** (塑形器) -> {目标语言}
        - **Isle Bucks** (岛民券) -> {目标语言}
        - **Great Token** (伟大代币) -> {目标语言}
        - **Coins** (硬币) -> {目标语言}`;

  // await $`trans-llm translate ${potPath} ${targetLang} ${outputPath} --batch-size 300 --end-point ${endPoint} --api-key ${apiKey} --model ${model} --custom_prompt "${custom_prompt}" --skip-translated`;
  await $`trans-llm translate ${potPath} ${targetLang} ${outputPath} \
    --batch-size 300 \
    --end-point ${endPoint} \
    --api-key ${apiKey} \
    --model ${model} \
    --custom_prompt ${custom_prompt} \
    --skip-translated`;
}

const tasks: Record<string, { desc: string; action: () => Promise<void> }> = {
  compile: {
    desc: "Compile project",
    action: async () => { await $`./gradlew compileKotlinJvm` },
  },
  build: {
    desc: "Build the game distributable in debug mode",
    action: async () => {
      await performBuild(false);
    },
  },
  "build:release": {
    desc: "Build the game distributable in release mode",
    action: async () => {
      await performBuild(true);
    },
  },
  "i18n:clean": {
    desc: "Clean obsolete terms",
    action: async () => {
      const glob = new Glob("**/*.po");
      for await (const file of glob.scan(
        "sharedUI/src/commonMain/composeResources/files/i18n/",
      )) {
        console.log(`清理... ${file}`);
        await $`msgmerge sharedUI/src/commonMain/composeResources/files/i18n/${file} sharedUI/src/commonMain/composeResources/files/i18n/messages.pot`;
      }
    },
  },
  "i18n:extract": {
    desc: "Extract strings to POT file",
    action: async () => {
      await $`bun run tools/extract-po/extract_pot.ts`;
    },
  },
  "i18n:translate": {
    desc: "Translate PO files using AI",
    action: async () => {
      await tasks["i18n:extract"].action();
      await translate_task("English");
      await translate_task("SimplifiedChinese");

      await translate_task("Korean");
      await translate_task("Arabic");
      await translate_task("Czech");
      await translate_task("Dutch");
      await translate_task("French");
      await translate_task("German");
      await translate_task("Hungarian");
      await translate_task("Italian");
      await translate_task("Japanese");
      await translate_task("Norwegian");
      await translate_task("Polish");
      await translate_task("Portugal");
      await translate_task("Portuguese-Brazil");
      await translate_task("Russian");
      await translate_task("Spanish");
      await translate_task("Spanish-Latin-America");
      await translate_task("TraditionalChinese");
      await translate_task("Turkish");
      await translate_task("Ukrainian");
    },
  },
  "steam:login": {
    desc: "Login to SteamCMD",
    action: async () => {
      await $`${getSteamCmdPath()} +login ${CONFIG.steam.account}`;
    },
  },
  "steam:publish": {
    desc: "Publish release build to Steam (Main Branch)",
    action: async () => {
      await ensureExecutable();
      const scriptPath = getVdfPath("publish");
      await $`${getSteamCmdPath()} +login ${CONFIG.steam.account} +run_app_build ${scriptPath} +quit`;
    },
  },
  "steam:publish_dlc": {
    desc: "Publish dlc to Steam (Main Branch)",
    action: async () => {
      await ensureExecutable();
      const scriptPath = getVdfPath("dlc_publish");
      await $`${getSteamCmdPath()} +login ${CONFIG.steam.account} +run_app_build ${scriptPath} +quit`;
    },
  },
  "steam:publish-preview": {
    desc: "Publish to Steam (Preview)",
    action: async () => {
      await ensureExecutable();
      const scriptPath = getVdfPath("preview");
      await $`${getSteamCmdPath()} +login ${CONFIG.steam.account} +run_app_build ${scriptPath} +quit`;
    },
  },
  "steam:publish-local": {
    desc: "Publish to Local Steam Server",
    action: async () => {
      await ensureExecutable();
      const scriptPath = getVdfPath("local");
      await $`${getSteamCmdPath()} +login ${CONFIG.steam.account} +run_app_build ${scriptPath} +quit`;
    },
  },
  "steam:publish_all": {
    desc: "Publish ALL release build to Steam (Main Branch)",
    action: async () => {
      await ensureExecutable();
      const scriptPath = getVdfPath("publish", false);
      await $`${getSteamCmdPath()} +login ${CONFIG.steam.account} +run_app_build ${scriptPath} +quit`;
    },
  },

  luban: {
    desc: "Generate data files",
    action: async () => {
      const lubanDLL = "tools/luban/app/Luban.dll";
      const lubanConfig = "tools/luban/luban.conf";
      const output =
        "sharedUI/src/commonMain/composeResources/files/tables/data";

      await $`dotnet "${lubanDLL}" -t all -d yaml --conf "${lubanConfig}" -x outputDataDir="${output}"`;
    },
  },

  "host-server": {
    desc: "Start the server",
    action: async () => {
      const server = Bun.serve({
        port: 8080,
        async fetch(req) {
          const url = new URL(req.url);
          let path = url.pathname;

          if (path === "/") path = "/index.html";

          const filePath = `${"tools/ContentServer"}${path}`;
          const file = Bun.file(filePath);

          if (file.exists()) {
            return new Response(file);
          }

          return new Response("404 Not Found", { status: 404 });
        },
      });

      console.log(`Server started on localhost:${server.port}`);
    },
  },

  steam_localhost: {
    desc: "Launch Steam via dev mode (removes config on exit)",
    action: async () => {
      const isWin = isWindows();
      const STEAM_DEV_CFG = isWin
        ? join("C:", "Program Files (x86)", "Steam", "steam_dev.cfg")
        : join(homedir(), ".local/share/Steam/steam_dev.cfg");

      const CONTENT_SERVER_URL = `@LocalContentServer "localhost:8080"\n`;

      try {
        log.info(`Creating dev config at: ${STEAM_DEV_CFG}`);
        await Bun.write(STEAM_DEV_CFG, CONTENT_SERVER_URL); // Using Bun's native write
        log.success("🚀 steam_dev.cfg created.");

        log.info("---------------------------------------------------");
        log.info(" STATUS: Local Dev Mode ACTIVE");
        log.info(" Press [Ctrl+C] to stop and auto-delete config file");
        log.info("---------------------------------------------------");

        // Keep the process alive
        return new Promise<void>((resolve) => {
          const cleanup = async () => {
            console.log("\n"); // New line for cleaner log after ^C
            log.step("Cleaning up steam_dev.cfg...");
            try {
              await rm(STEAM_DEV_CFG, { force: true });
              log.success("Cleanup complete. Steam is back to normal.");
            } catch (e) {
              log.error("Cleanup failed. Please delete the file manually.");
            }
            process.exit(0);
          };

          // Listen for exit signals
          process.on("SIGINT", cleanup);
          process.on("SIGTERM", cleanup);

          // THIS IS THE FIX: A tiny interval keeps the event loop busy
          // so Bun doesn't think the program is "finished".
          const keepAlive = setInterval(() => {}, 1000 * 60 * 60);

          // Optional: If you want to allow pressing 'Enter' to quit too
          process.stdin.resume();
          process.stdin.on("data", cleanup);
        });
      } catch (err: any) {
        log.error(`Failed: ${err.message}`);
        if (isWin) log.info("Note: Try running as Administrator.");
      }
    },
  },
};

async function main() {
  const command = Bun.argv[2];

  if (!command || command === "--help" || command === "-h") {
    console.log("\n🛠️  \x1b[1mAvailable Tasks:\x1b[0m");
    const tableData = Object.entries(tasks).map(([name, { desc }]) => ({
      Task: `\x1b[33m${name}\x1b[0m`,
      Description: desc,
    }));
    console.table(tableData);
    return;
  }

  const task = tasks[command];
  if (task) {
    try {
      log.info(`Starting task: ${command}`);
      const start = performance.now();
      await task.action();
      const end = performance.now();
      log.success(
        `Task '${command}' completed in ${((end - start) / 1000).toFixed(2)}s`,
      );
    } catch (err) {
      log.error(`Task '${command}' failed!`);
      console.error(err);
      process.exit(1);
    }
  } else {
    log.error(`Unknown task: ${command}`);
    console.log("Run with --help to see available tasks.");
    process.exit(1);
  }
}

main();
