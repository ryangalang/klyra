import test from 'node:test';
import assert from 'node:assert/strict';
import { stat, open, readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { createHash } from 'node:crypto';

// Integration checks against a running local preview and the actual v29 artifacts.
const base = process.env.TEST_BASE_URL || 'http://127.0.0.1:3000';
const directory = process.env.KLYRA_LOCAL_RELEASE_DIR || resolve('../../artifacts/Klyra-v29-2026-10-29');
const files = {
  android: 'Klyra-Android-arm64.apk', universal: 'Klyra-Android-universal.apk',
  arm32: 'Klyra-Android-arm32.apk', emulator: 'Klyra-Android-x64-emulator.apk',
  windows: 'Klyra-Windows-x64.zip', source: 'Klyra-corresponding-source.zip', checksums: 'SHA256SUMS.txt',
};
for (const [key, file] of Object.entries(files)) {
  test(`${key}: attachment headers and resumable bytes match actual release file`, async () => {
    const info = await stat(resolve(directory, file));
    const response = await fetch(`${base}/download/${key}`, { method: 'HEAD', redirect: 'manual' });
    assert.equal(response.status, 200, 'Local file must download, not redirect to an unavailable release');
    assert.equal(Number(response.headers.get('content-length')), info.size);
    assert.equal(response.headers.get('content-disposition'), `attachment; filename="${file}"`);
    const part = await fetch(`${base}/download/${key}`, { headers: { Range: 'bytes=0-31' } });
    assert.equal(part.status, 206);
    assert.equal(part.headers.get('content-range'), `bytes 0-31/${info.size}`);
    const handle = await open(resolve(directory, file));
    try {
      const expected = Buffer.alloc(32); await handle.read(expected, 0, 32, 0);
      assert.deepEqual(Buffer.from(await part.arrayBuffer()), expected);
    } finally { await handle.close(); }
  });
}
test('invalid downloads and unsatisfiable ranges fail safely', async () => {
  assert.equal((await fetch(`${base}/download/not-a-release`)).status, 404);
  assert.equal((await fetch(`${base}/download/android`, { headers: { Range: 'bytes=999999999-' } })).status, 416);
});
test('complete APK HTTP body matches the published SHA-256 checksum', async () => {
  const response = await fetch(`${base}/download/android`);
  assert.equal(response.status, 200);
  const hash = createHash('sha256');
  for await (const chunk of response.body) hash.update(chunk);
  assert.equal(hash.digest('hex'), '9dfed8f724c3b9c51c9c6e6bbe2d2ec0570979f19a98cd2e30e4614b7ba45e93');
});
test('checksum download matches the release manifest byte-for-byte', async () => {
  const response = await fetch(`${base}/download/checksums`);
  assert.deepEqual(Buffer.from(await response.arrayBuffer()), await readFile(resolve(directory, files.checksums)));
});
