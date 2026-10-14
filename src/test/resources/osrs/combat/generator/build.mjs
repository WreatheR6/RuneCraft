import * as esbuild from '../deps/node_modules/esbuild/lib/main.js';
import path from 'path';
import fs from 'fs';
const HERE = path.dirname(new URL(import.meta.url).pathname);
const SRC = path.resolve(HERE, '../src/osrs-dps-calc/src');
const DEPS = path.resolve(HERE, '../deps/node_modules');
const real = new Set(['fs', 'path', 'd3-array', 'lodash.mergewith', 'lodash.isequal', 'internmap']);
function resolveFile(p) {
  for (const ext of ['', '.ts', '.tsx', '/index.ts', '/index.tsx']) {
    if (fs.existsSync(p + ext) && fs.statSync(p + ext).isFile()) return p + ext;
  }
  throw new Error('cannot resolve ' + p);
}
await esbuild.build({
  entryPoints: [path.resolve(HERE, process.argv[2])], bundle: true, platform: 'node', format: 'cjs',
  outfile: path.resolve(HERE, process.argv[3]),
  loader: { '.png': 'empty', '.webp': 'empty', '.svg': 'empty', '.css': 'empty' },
  nodePaths: [DEPS], logLevel: 'warning',
  plugins: [{ name: 'alias', setup(b) {
    b.onResolve({ filter: /^@\// }, a => ({ path: resolveFile(path.join(SRC, a.path.slice(2))) }));
    b.onResolve({ filter: /^[a-z@][^:]*$/ }, a => {
      const root = a.path.startsWith('@') ? a.path.split('/').slice(0, 2).join('/') : a.path.split('/')[0];
      if (real.has(root)) return undefined;
      return { path: a.path, namespace: 'stub' };
    });
    b.onLoad({ filter: /.*/, namespace: 'stub' }, () => ({
      contents: 'const f = function(){}; module.exports = new Proxy(f, {get:(t,k)=>k==="__esModule"?false:(k==="default"?module.exports:undefined)});',
      loader: 'js',
    }));
  } }],
});
