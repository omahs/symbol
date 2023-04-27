#!/bin/bash

set -ex

bash scripts/run_catbuffer_generator.sh dryrun

cd wasm
rustup default stable
wasm-pack build --release --no-typescript --target nodejs --out-dir ../_build/wasm/node
wasm-pack build --release --no-typescript --target web --out-dir ../_build/wasm/web
cd ..

npm run bundle

sed -i 's/symbol-crypto-wasm/symbol-crypto-wasm-node/g' _build/wasm/node/package.json
sed -i 's/symbol-crypto-wasm/symbol-crypto-wasm-web/g' _build/wasm/web_webpack/package.json
