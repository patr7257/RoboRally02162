// Copies the built RoboRally engine ESM bundle from the sibling engine/ package
// into client/src/engine/ so CRA (whose ModuleScopePlugin forbids imports from
// outside src/) can bundle it. The hand-authored roborally-engine.d.ts next to
// the copied bundle supplies the types; only the .js is copied here.
//
// Run automatically via the prebuild/prestart npm hooks, or manually:
//     npm run sync-engine
//
// Requires the engine to be built first (cd ../engine; pnpm build).

import { existsSync, mkdirSync, copyFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const src = resolve(here, "..", "..", "engine", "dist", "roborally-engine.js");
const destDir = resolve(here, "..", "src", "engine");
const dest = resolve(destDir, "roborally-engine.js");

if (!existsSync(src)) {
  console.error(
    "[sync-engine] engine bundle not found at:\n  " +
      src +
      "\nBuild the engine first:  cd ../engine && pnpm build",
  );
  process.exit(1);
}

mkdirSync(destDir, { recursive: true });
copyFileSync(src, dest);
console.log("[sync-engine] copied engine bundle -> " + dest);
