import { Glob } from "bun";
import { readFileSync, writeFileSync, existsSync, mkdirSync } from "node:fs";
import { basename, dirname, resolve } from "node:path";
import { parse } from "yaml";

const SCAN_DIRS = [
    "sharedUI/src/commonMain/composeResources/files/tables",
    "sharedUI/src/commonMain/kotlin",
];

const OUTPUT_FILE =
    "sharedUI/src/commonMain/composeResources/files/i18n/messages.pot";
const YAML_FIELDS = ["name", "category", "subCategory", "desc"];
const TARGET_FUNCTIONS = ["tr", "i18nWrapper", "i18nWrapperContext"];

const IGNORE_YAML_FILES = [
    "alchemyTalents.yaml",
    "combatTalents.yaml",
    "fishingAndCookingTalents.yaml",
    "huntingAndChartingTalents.yaml",
    "miningAndSmeltingTalents.yaml",
    "talentTemplates.yaml",
    "woodcuttingTalents.yaml",
    "episodes.yaml",
//     "dlcsocietalitems.yml",
//     "policyCards.yaml",
//     "buildingtemplates.yml",
];

// 最终存储：Map<"context|text", { context, text, locations }>
const entries: Map<
    string,
    { context: string | null; text: string; locations: Set<string> }
> = new Map();

function addEntry(
    text: string,
    fileName: string,
    context: string | null = null,
) {
    const val = text.trim();
    if (!val) return;

    const key = context ? `${context}\u0000${val}` : `\u0000${val}`;

    if (!entries.has(key)) {
        entries.set(key, { context, text: val, locations: new Set() });
    }
    entries.get(key)!.locations.add(fileName);
}

/**
 * 处理 Kotlin 代码中的 tr() 和 i18nWrapper
 */
function extractFromKotlin(content: string, fileName: string) {
    for (const fnName of TARGET_FUNCTIONS) {
        let index = 0;
        while ((index = content.indexOf(`${fnName}(`, index)) !== -1) {
            const startArgsIndex = index + fnName.length + 1;
            const argsContent = getBalancedContent(content, startArgsIndex);

            if (argsContent) {
                const strings = extractStringLiterals(argsContent);
                if (strings.length > 0) {
                    if (
                        fnName === "i18nWrapperContext" &&
                        strings.length >= 2
                    ) {
                        addEntry(strings[1], fileName, strings[0]);
                    } else {
                        addEntry(strings[0], fileName);
                    }
                }
            }
            index += fnName.length;
        }
    }
}

// 辅助：获取括号平衡内容
function getBalancedContent(
    content: string,
    startIndex: number,
): string | null {
    let depth = 1,
        i = startIndex,
        inString: string | null = null,
        isEscaped = false;
    while (i < content.length && depth > 0) {
        const char = content[i];
        if ((char === '"' || char === "'") && !isEscaped) {
            if (!inString) inString = char;
            else if (inString === char) inString = null;
        }
        if (!inString) {
            if (char === "(") depth++;
            else if (char === ")") depth--;
        }
        isEscaped = char === "\\" && !isEscaped;
        i++;
    }
    return depth === 0 ? content.substring(startIndex, i - 1) : null;
}

// 辅助：提取字符串字面量
function extractStringLiterals(argsStr: string): string[] {
    const result: string[] = [];
    let i = 0;
    while (i < argsStr.length) {
        const char = argsStr[i];
        if (char === '"' || char === "'") {
            const quoteType = char;
            let str = "";
            i++;
            while (i < argsStr.length) {
                if (argsStr[i] === quoteType && argsStr[i - 1] !== "\\") {
                    result.push(str);
                    break;
                }
                str += argsStr[i];
                i++;
            }
        }
        i++;
    }
    return result;
}

function preProcessYaml(content: string) {
    return content.replace(/!<\w+>/g, "").replace(/!\w+/g, "");
}

function recursiveExtract(data: any, fileName: string) {
    if (!data || typeof data !== "object") return;

    if (Array.isArray(data)) {
        data.forEach((item) => recursiveExtract(item, fileName));
        return;
    }

    for (const key in data) {
        const value = data[key];

        if (YAML_FIELDS.includes(key) && value) {
            if (typeof value === "string") {
                addEntry(value, fileName);
            } else if (typeof value === "object") {
                recursiveExtract(value, fileName);
            }
        } else {
            recursiveExtract(value, fileName);
        }
    }
}

async function run() {
    console.log(`🚀 开始扫描最新词条`);

    for (const dir of SCAN_DIRS) {
        if (!existsSync(dir)) continue;
        const glob = new Glob("**/*.{yaml,yml,kt}");

        for await (const file of glob.scan(dir)) {
            const fileNameOnly = basename(file);
            if (IGNORE_YAML_FILES.includes(fileNameOnly)) continue;

            const filePath = resolve(dir, file);
            const rawContent = readFileSync(filePath, "utf8");

            if (file.endsWith(".yaml") || file.endsWith(".yml")) {
                try {
                    const cleanContent = preProcessYaml(rawContent);
                    const data = parse(cleanContent);
                    recursiveExtract(data, file);
                } catch (e) {
                    console.error(`❌ YAML 解析失败: ${file}`, e);
                }
            } else if (file.endsWith(".kt")) {
                extractFromKotlin(rawContent, file);
            }
        }
    }

    savePotFile();
}

/**
 * 保存 POT 文件
 * 这里的逻辑会自动移除 entries 中不存在的（即扫描不到的）旧词条
 */
function savePotFile() {
    let potContent = `msgid ""\nmsgstr ""\n"Content-Type: text/plain; charset=UTF-8\\n"\n"Content-Transfer-Encoding: 8bit\\n"\n\n`;

    // 直接遍历当前扫描到的 entries
    // 因为 entries 是在 run() 中从零开始填充的，所以没被扫描到的旧词条自然不会出现在这里
    for (const item of entries.values()) {
        potContent += `#. Files: ${Array.from(item.locations).join(", ")}\n`;
        if (item.context) {
            potContent += `msgctxt "${item.context.replace(/"/g, '\\"')}"\n`;
        }
        const safeId = item.text.replace(/"/g, '\\"').replace(/\n/g, "\\n");
        potContent += `msgid "${safeId}"\nmsgstr ""\n\n`;
    }

    const outDir = dirname(OUTPUT_FILE);
    if (!existsSync(outDir)) mkdirSync(outDir, { recursive: true });

    // 写入文件（直接覆盖原有文件，实现删除不存在词条的效果）
    writeFileSync(OUTPUT_FILE, potContent, "utf8");

    console.log(`✅ 清理并更新完成！当前有效词条共 ${entries.size} 条`);
    console.log(`📍 文件保存至: ${resolve(OUTPUT_FILE)}`);
}

run().catch(console.error);
