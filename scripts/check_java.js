const { parse } = require("java-parser");
const fs = require("fs");
const path = require("path");
const root = process.argv[2];
let files = [];
(function walk(d) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith(".java")) files.push(p);
  }
})(root);
let bad = 0;
for (const f of files.sort()) {
  try { parse(fs.readFileSync(f, "utf8")); }
  catch (e) {
    bad++;
    const m = (e.message || "").split("\n").slice(0, 3).join(" | ");
    console.log("FAIL " + path.relative(root, f) + "\n     " + m);
  }
}
console.log(`\n${files.length} Java files parsed, ${bad} with syntax errors`);
process.exit(bad ? 1 : 0);
