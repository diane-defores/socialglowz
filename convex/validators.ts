const ID_PATTERN = /^[a-zA-Z0-9:_-]+$/;
const NETWORK_ID_PATTERN = /^[a-z0-9-]+$/;
const LANGUAGE_PATTERN = /^[a-z]{2}(?:-[A-Z]{2})?$/;
const DATA_URL_IMAGE_PREFIX = /^data:image\/[a-zA-Z0-9.+-]+;base64,/;

export const LIMITS = {
  idMax: 128,
  networkIdMax: 32,
  labelMax: 80,
  profileNameMax: 64,
  emojiMax: 16,
  avatarMax: 300_000,
  customLinkUrlMax: 2048,
  customLinkIconMax: 64,
  hiddenNetworksMax: 32,
  friendNameMax: 80,
  friendNamesMax: 200,
} as const;

function assertTrimmed(value: string, field: string) {
  if (value.trim() !== value) {
    throw new Error(`${field} must not include leading/trailing spaces`);
  }
}

function assertLength(value: string, field: string, max: number) {
  if (value.length === 0 || value.length > max) {
    throw new Error(`${field} length must be between 1 and ${max}`);
  }
}

export function assertEntityId(value: string, field: string) {
  assertTrimmed(value, field);
  assertLength(value, field, LIMITS.idMax);
  if (!ID_PATTERN.test(value)) {
    throw new Error(`${field} contains unsupported characters`);
  }
}

export function assertNetworkId(networkId: string) {
  assertTrimmed(networkId, "networkId");
  assertLength(networkId, "networkId", LIMITS.networkIdMax);
  if (!NETWORK_ID_PATTERN.test(networkId)) {
    throw new Error("networkId contains unsupported characters");
  }
}

export function assertLabel(label: string, field = "label") {
  assertTrimmed(label, field);
  assertLength(label, field, LIMITS.labelMax);
}

export function assertProfileName(name: string) {
  assertTrimmed(name, "profile name");
  assertLength(name, "profile name", LIMITS.profileNameMax);
}

export function assertEmoji(emoji: string) {
  assertTrimmed(emoji, "emoji");
  assertLength(emoji, "emoji", LIMITS.emojiMax);
}

export function assertAvatar(avatar?: string) {
  if (avatar === undefined) return;
  assertTrimmed(avatar, "avatar");
  if (avatar.length > LIMITS.avatarMax) {
    throw new Error(`avatar length must be <= ${LIMITS.avatarMax}`);
  }
  if (avatar.startsWith("data:")) {
    if (!DATA_URL_IMAGE_PREFIX.test(avatar)) {
      throw new Error("avatar data URL must be a base64 image");
    }
    return;
  }
  let url: URL;
  try {
    url = new URL(avatar);
  } catch {
    throw new Error("avatar must be a valid URL or image data URL");
  }
  if (url.protocol !== "http:" && url.protocol !== "https:") {
    throw new Error("avatar URL must use http or https");
  }
}

export function assertHiddenNetworks(hiddenNetworks?: string[]) {
  if (!hiddenNetworks) return;
  if (hiddenNetworks.length > LIMITS.hiddenNetworksMax) {
    throw new Error(`hiddenNetworks length must be <= ${LIMITS.hiddenNetworksMax}`);
  }
  for (const networkId of hiddenNetworks) {
    assertNetworkId(networkId);
  }
}

export function assertHttpUrl(urlValue: string) {
  assertTrimmed(urlValue, "url");
  if (urlValue.length > LIMITS.customLinkUrlMax) {
    throw new Error(`url length must be <= ${LIMITS.customLinkUrlMax}`);
  }
  if (urlValue.includes("\n") || urlValue.includes("\r")) {
    throw new Error("url must not contain line breaks");
  }
  let url: URL;
  try {
    url = new URL(urlValue);
  } catch {
    throw new Error("url must be a valid absolute URL");
  }
  if (url.protocol !== "http:" && url.protocol !== "https:") {
    throw new Error("url protocol must be http or https");
  }
}

export function assertIcon(icon: string) {
  assertTrimmed(icon, "icon");
  assertLength(icon, "icon", LIMITS.customLinkIconMax);
}

export function assertLanguage(language: string) {
  assertTrimmed(language, "language");
  if (!LANGUAGE_PATTERN.test(language)) {
    throw new Error("language must match xx or xx-YY");
  }
}

export function assertTextZoom(level: number) {
  if (!Number.isFinite(level)) {
    throw new Error("textZoom must be a finite number");
  }
  if (level < 50 || level > 200) {
    throw new Error("textZoom must be between 50 and 200");
  }
}

export function assertFriendNames(names: string[]) {
  if (names.length > LIMITS.friendNamesMax) {
    throw new Error(`names length must be <= ${LIMITS.friendNamesMax}`);
  }

  const seen = new Set<string>();
  for (const name of names) {
    assertTrimmed(name, "friend name");
    assertLength(name, "friend name", LIMITS.friendNameMax);
    const normalized = name.toLowerCase();
    if (seen.has(normalized)) {
      throw new Error("friend names must be unique (case-insensitive)");
    }
    seen.add(normalized);
  }
}
