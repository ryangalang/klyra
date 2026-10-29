export const release = {
  version: "1.8-klyra.4", code: 29, date: "October 29, 2026",
  repository: "ryangalang/klyra", tag: "v1.8-klyra.4",
};
export const assets = {
  android: { file: "Klyra-Android-arm64.apk", label: "Android · ARM64", size: "51.8 MB", sha256: "9dfed8f724c3b9c51c9c6e6bbe2d2ec0570979f19a98cd2e30e4614b7ba45e93" },
  universal: { file: "Klyra-Android-universal.apk", label: "Android · Universal", size: "149.4 MB", sha256: "4e6f9fc1f9486a01f9b5927b6ec2c4caf95f388c0d3ed5b54751e69d1c26f71d" },
  arm32: { file: "Klyra-Android-arm32.apk", label: "Android · ARM32", size: "42.9 MB", sha256: "261f45b5f5b1da90e91575c1b3b0497f130e621df2a70ea1b88425a0ab2d1fc6" },
  emulator: { file: "Klyra-Android-x64-emulator.apk", label: "Android · x86-64", size: "58.0 MB", sha256: "06e771feb9597b8ccf1c6ff66c49666c837bb7252b7996a408b5a1dbce465734" },
  windows: { file: "Klyra-Windows-x64.zip", label: "Windows · x64", size: "311.6 MB", sha256: "d53b116a356da0d78d516a9f3687ff5768481f6f15fff072098a5ccca11e1932" },
  source: { file: "Klyra-corresponding-source.zip", label: "Corresponding app source", size: "15.1 MB", sha256: "0728ae12b3f5462ccb35dba5efe09ec8ab3a8c2bdbacc420344731c172c8b308" },
  checksums: { file: "SHA256SUMS.txt", label: "SHA-256 checksums", size: "562 B", sha256: "" },
} as const;
export type AssetKey = keyof typeof assets;
export function isAssetKey(value: string): value is AssetKey { return Object.hasOwn(assets, value); }
export function assetUrl(key: AssetKey) {
  return `https://github.com/${release.repository}/releases/download/${release.tag}/${assets[key].file}`;
}
