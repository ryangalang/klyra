export const release = {
  version: "1.8-klyra.7", androidVersion: "1.8-klyra.7", code: 30, date: "November 2, 2026",
  repository: process.env.KLYRA_DOWNLOAD_REPOSITORY || "ryangalang/klyra", tag: "v1.8-klyra.7",
};
export const assets = {
  android: { file: "Klyra-Android-arm64.apk", label: "Android · ARM64", size: "51.9 MB", sha256: "01cd6bf52b500c3e9047f1f58fbdea4e7f35283de90f003096705ace9b8fa8bb" },
  universal: { file: "Klyra-Android-universal.apk", label: "Android · Universal", size: "149.4 MB", sha256: "ab71d2cdfecb4daa09905fb8537343b923eb9806dc677c66da87324e5ffb5216" },
  arm32: { file: "Klyra-Android-arm32.apk", label: "Android · ARM32", size: "42.9 MB", sha256: "43294d9c7c8fe1ac7715fc41f6c351b2d6ecbacbb095322c69c5d39a01ad2651" },
  emulator: { file: "Klyra-Android-x64-emulator.apk", label: "Android · x86-64", size: "58.1 MB", sha256: "15c6de95e72f4dc27a1018a1df5dc088a5ba813e760c1627fc67f0d93e6af279" },
  windows: { file: "Klyra-Windows-x64-setup.exe", label: "Windows · x64", size: "312.2 MB", sha256: "273307a7b98cc6f6ff4db2a7816a73986c216150f0fddcfc10317143fa00c75a" },
  source: { file: "Klyra-corresponding-source.zip", label: "Corresponding app source", size: "15.3 MB", sha256: "7ad9fa9fed6f9394461b6348d085a2b1af4e584372842e9b8ea83945152ab891" },
  checksums: { file: "SHA256SUMS.txt", label: "SHA-256 checksums", size: "657 B", sha256: "" },
} as const;
export type AssetKey = keyof typeof assets;
export function isAssetKey(value: string): value is AssetKey { return Object.hasOwn(assets, value); }
export function assetUrl(key: AssetKey) {
  return `https://github.com/${release.repository}/releases/download/${release.tag}/${assets[key].file}`;
}
