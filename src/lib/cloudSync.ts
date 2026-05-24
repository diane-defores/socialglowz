import { getConvexClient } from "@/lib/convex";
import { isAuthenticated } from "@/lib/convexAuth";
import { syncSettingsPatch } from "@/lib/cloudSettings";
import {
  clearCloudSyncQueue,
  flushCloudSyncQueue,
  hasPendingCloudSync,
} from "@/lib/cloudSyncQueue";
import { api } from "../../convex/_generated/api";
import { useAccountsStore } from "@/stores/accounts";
import { useProfilesStore } from "@/stores/profiles";
import { useCustomLinksStore } from "@/stores/customLinks";
import { useFriendsFilterStore } from "@/stores/friendsFilter";
import { useThemeStore } from "@/stores/theme";
import { useOnboardingStore } from "@/stores/onboarding";
import { setLocale } from "@/utils/i18n";
import {
  advancePostAuthSyncStage,
  beginPostAuthSyncFeedback,
  queuePostAuthReadyNotice,
  resetPostAuthSyncFeedback,
  showPostAuthReadyFeedback,
} from "@/lib/postAuthSyncFeedback";
import type { ThemeMode } from "@/utils/themeAuto";
import { normalizeTapSoundVariant } from "@/ui/setup/pages/SocialGlowz/utils/tapSound";
import {
  canReuseLocalCloudState,
  isCloudSnapshotEmpty,
  shouldKeepLocalWhenCloudEmpty,
  type CloudSnapshotShape,
} from "@/lib/cloudSyncDecisions";
import type { CloudSettingsPatch } from "@/lib/cloudSettings";

type CloudSnapshot = CloudSnapshotShape & {
  settings: CloudSettings | null;
  profiles: CloudProfile[];
  customLinks: CloudCustomLink[];
  friendsFilters: CloudFriendFilter[];
  socialAccounts: CloudSocialAccount[];
  activeAccounts: CloudActiveAccount[];
};

type CloudSettings = Pick<
  CloudSettingsPatch,
  | "theme"
  | "language"
  | "grayscaleEnabled"
  | "textZoom"
  | "hapticEnabled"
  | "tapSoundEnabled"
  | "tapSoundVariant"
  | "activeProfileId"
  | "onboardingCompleted"
  | "friendsFilterEnabled"
>;

type CloudProfile = {
  profileId: string;
  name: string;
  emoji: string;
  avatar?: string;
  hiddenNetworks?: string[];
  createdAt: number;
};

type CloudCustomLink = {
  linkId: string;
  profileId: string;
  label: string;
  url: string;
  icon: string;
};

type CloudFriendFilter = {
  networkId: string;
  names: string[];
};

type CloudSocialAccount = {
  accountId: string;
  networkId: string;
  label: string;
  addedAt: number;
};

type CloudActiveAccount = {
  networkId: string;
  accountId: string;
};

const ID_PATTERN = /^[a-zA-Z0-9:_-]+$/;
const NETWORK_ID_PATTERN = /^[a-z0-9-]+$/;
const LANGUAGE_PATTERN = /^[a-z]{2}(?:-[A-Z]{2})?$/;
const IMAGE_DATA_URL_PATTERN = /^data:image\/[a-zA-Z0-9.+-]+;base64,/;
const ID_MAX = 128;
const NETWORK_ID_MAX = 32;
const LABEL_MAX = 80;
const PROFILE_NAME_MAX = 64;
const EMOJI_MAX = 16;
const AVATAR_MAX = 300_000;
const CUSTOM_LINK_URL_MAX = 2048;
const CUSTOM_LINK_ICON_MAX = 64;
const HIDDEN_NETWORKS_MAX = 32;
const FRIEND_NAME_MAX = 80;
const FRIEND_NAMES_MAX = 200;
const TEXT_ZOOM_MIN = 50;
const TEXT_ZOOM_MAX = 200;

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === "object" && !Array.isArray(value);
}

function isTrimmedBoundedString(value: unknown, max: number): value is string {
  return typeof value === "string"
    && value.length > 0
    && value.length <= max
    && value.trim() === value;
}

function isEntityId(value: unknown): value is string {
  return isTrimmedBoundedString(value, ID_MAX) && ID_PATTERN.test(value);
}

function isNetworkId(value: unknown): value is string {
  return isTrimmedBoundedString(value, NETWORK_ID_MAX) && NETWORK_ID_PATTERN.test(value);
}

function isHttpUrl(value: unknown): value is string {
  if (
    !isTrimmedBoundedString(value, CUSTOM_LINK_URL_MAX)
    || value.includes("\n")
    || value.includes("\r")
  ) {
    return false;
  }

  try {
    const url = new URL(value);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

function isAvatar(value: unknown): value is string | undefined {
  if (value === undefined) return true;
  if (!isTrimmedBoundedString(value, AVATAR_MAX)) return false;
  if (value.startsWith("data:")) return IMAGE_DATA_URL_PATTERN.test(value);
  return isHttpUrl(value);
}

function isThemeMode(value: unknown): value is ThemeMode {
  return value === "light" || value === "dark" || value === "auto";
}

function isTapSoundVariant(value: unknown): value is NonNullable<CloudSettings["tapSoundVariant"]> {
  return value === "classic" || value === "soft" || value === "pop";
}

function isTextZoom(value: unknown): value is number {
  return typeof value === "number"
    && Number.isFinite(value)
    && value >= TEXT_ZOOM_MIN
    && value <= TEXT_ZOOM_MAX;
}

function asStringArray(
  value: unknown,
  itemGuard: (item: unknown) => item is string,
  max: number,
) {
  if (!Array.isArray(value) || value.length > max) return null;
  const next: string[] = [];
  const seen = new Set<string>();

  for (const item of value) {
    if (!itemGuard(item) || typeof item !== "string") return null;
    const normalized = item.toLowerCase();
    if (seen.has(normalized)) return null;
    seen.add(normalized);
    next.push(item);
  }

  return next;
}

export function asCloudSettings(value: unknown): CloudSettings | null {
  if (!isRecord(value)) {
    return null;
  }

  const settings: CloudSettings = {};

  if (isThemeMode(value.theme)) settings.theme = value.theme;
  if (typeof value.language === "string" && LANGUAGE_PATTERN.test(value.language)) {
    settings.language = value.language;
  }
  if (typeof value.grayscaleEnabled === "boolean") settings.grayscaleEnabled = value.grayscaleEnabled;
  if (isTextZoom(value.textZoom)) settings.textZoom = value.textZoom;
  if (typeof value.hapticEnabled === "boolean") settings.hapticEnabled = value.hapticEnabled;
  if (typeof value.tapSoundEnabled === "boolean") settings.tapSoundEnabled = value.tapSoundEnabled;
  if (isTapSoundVariant(value.tapSoundVariant)) settings.tapSoundVariant = value.tapSoundVariant;
  if (isEntityId(value.activeProfileId)) settings.activeProfileId = value.activeProfileId;
  if (typeof value.onboardingCompleted === "boolean") {
    settings.onboardingCompleted = value.onboardingCompleted;
  }
  if (typeof value.friendsFilterEnabled === "boolean") {
    settings.friendsFilterEnabled = value.friendsFilterEnabled;
  }

  return Object.keys(settings).length > 0 ? settings : null;
}

function asCloudProfile(value: unknown): CloudProfile | null {
  if (!isRecord(value)) return null;
  if (
    !isEntityId(value.profileId)
    || !isTrimmedBoundedString(value.name, PROFILE_NAME_MAX)
    || !isTrimmedBoundedString(value.emoji, EMOJI_MAX)
    || !isAvatar(value.avatar)
    || typeof value.createdAt !== "number"
    || !Number.isFinite(value.createdAt)
  ) {
    return null;
  }

  const hiddenNetworks = value.hiddenNetworks === undefined
    ? undefined
    : asStringArray(value.hiddenNetworks, isNetworkId, HIDDEN_NETWORKS_MAX);
  if (hiddenNetworks === null) return null;

  return {
    profileId: value.profileId,
    name: value.name,
    emoji: value.emoji,
    avatar: typeof value.avatar === "string" ? value.avatar : undefined,
    hiddenNetworks,
    createdAt: value.createdAt,
  };
}

export function asCloudProfiles(value: unknown): CloudProfile[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) => {
    const profile = asCloudProfile(item);
    return profile ? [profile] : [];
  });
}

function asCloudCustomLink(value: unknown): CloudCustomLink | null {
  if (!isRecord(value)) return null;
  if (
    !isEntityId(value.linkId)
    || !isEntityId(value.profileId)
    || !isTrimmedBoundedString(value.label, LABEL_MAX)
    || !isHttpUrl(value.url)
    || !isTrimmedBoundedString(value.icon, CUSTOM_LINK_ICON_MAX)
  ) {
    return null;
  }

  return {
    linkId: value.linkId,
    profileId: value.profileId,
    label: value.label,
    url: value.url,
    icon: value.icon,
  };
}

export function asCloudCustomLinks(value: unknown): CloudCustomLink[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) => {
    const link = asCloudCustomLink(item);
    return link ? [link] : [];
  });
}

function asCloudFriendFilter(value: unknown): CloudFriendFilter | null {
  if (!isRecord(value) || !isNetworkId(value.networkId)) return null;
  const names = asStringArray(
    value.names,
    (item) => isTrimmedBoundedString(item, FRIEND_NAME_MAX),
    FRIEND_NAMES_MAX,
  );
  if (!names) return null;
  return {
    networkId: value.networkId,
    names,
  };
}

export function asCloudFriendFilters(value: unknown): CloudFriendFilter[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) => {
    const filter = asCloudFriendFilter(item);
    return filter ? [filter] : [];
  });
}

function asCloudSocialAccount(value: unknown): CloudSocialAccount | null {
  if (!isRecord(value)) return null;
  if (
    !isEntityId(value.accountId)
    || !isNetworkId(value.networkId)
    || !isTrimmedBoundedString(value.label, LABEL_MAX)
    || typeof value.addedAt !== "number"
    || !Number.isFinite(value.addedAt)
  ) {
    return null;
  }

  return {
    accountId: value.accountId,
    networkId: value.networkId,
    label: value.label,
    addedAt: value.addedAt,
  };
}

export function asCloudSocialAccounts(value: unknown): CloudSocialAccount[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) => {
    const account = asCloudSocialAccount(item);
    return account ? [account] : [];
  });
}

function asCloudActiveAccount(value: unknown): CloudActiveAccount | null {
  if (!isRecord(value)) return null;
  if (!isNetworkId(value.networkId) || !isEntityId(value.accountId)) return null;
  return {
    networkId: value.networkId,
    accountId: value.accountId,
  };
}

export function asCloudActiveAccounts(value: unknown): CloudActiveAccount[] {
  if (!Array.isArray(value)) return [];
  return value.flatMap((item) => {
    const account = asCloudActiveAccount(item);
    return account ? [account] : [];
  });
}

let hydratedUserId: string | null = null;
let hydratePromise: Promise<void> | null = null;
const REOPEN_SETTINGS_AFTER_AUTH_KEY = "sfz_reopen_settings_after_auth";
const CLOUD_SYNC_USER_ID_KEY = "sfz_cloud_sync_user_id";
const AUTH_RELOAD_DELAY_MS = 3000;

function canUseStorage() {
  return typeof window !== "undefined" && typeof localStorage !== "undefined";
}

function getRememberedCloudUserId() {
  if (!canUseStorage()) return null;
  return localStorage.getItem(CLOUD_SYNC_USER_ID_KEY);
}

function rememberCloudUserId(userId: string) {
  if (!canUseStorage()) return;
  localStorage.setItem(CLOUD_SYNC_USER_ID_KEY, userId);
}

function clearRememberedCloudUserId() {
  if (!canUseStorage()) return;
  localStorage.removeItem(CLOUD_SYNC_USER_ID_KEY);
}

async function fetchCloudSnapshot(client: ReturnType<typeof getConvexClient>): Promise<CloudSnapshot> {
  const [
    settings,
    profiles,
    customLinks,
    friendsFilters,
    socialAccounts,
    activeAccounts,
  ] = await Promise.all([
    client.query(api.settings.get, {}),
    client.query(api.profiles.list, {}),
    client.query(api.customLinks.list, {}),
    client.query(api.friendsFilters.list, {}),
    client.query(api.socialAccounts.list, {}),
    client.query(api.socialAccounts.listActive, {}),
  ]);

  return {
    settings: asCloudSettings(settings),
    profiles: asCloudProfiles(profiles),
    customLinks: asCloudCustomLinks(customLinks),
    friendsFilters: asCloudFriendFilters(friendsFilters),
    socialAccounts: asCloudSocialAccounts(socialAccounts),
    activeAccounts: asCloudActiveAccounts(activeAccounts),
  };
}

function applyCloudSettings(settings: CloudSettings | null) {
  const themeStore = useThemeStore();
  const profilesStore = useProfilesStore();
  const friendsStore = useFriendsFilterStore();
  const onboardingStore = useOnboardingStore();

  if (!settings) return false;

  themeStore.applyCloudPreferences(settings);

  if (typeof settings.language === "string") {
    setLocale(settings.language, false);
  }

  if (typeof settings.activeProfileId === "string") {
    profilesStore.activeProfileId = settings.activeProfileId;
  }

  if (typeof settings.friendsFilterEnabled === "boolean") {
    friendsStore.enabled = settings.friendsFilterEnabled;
  }

  if (typeof settings.onboardingCompleted === "boolean") {
    onboardingStore.completed = settings.onboardingCompleted;
  }

  return true;
}

function clearCloudBackedLocalState() {
  const profilesStore = useProfilesStore();
  const accountsStore = useAccountsStore();
  const customLinksStore = useCustomLinksStore();
  const friendsStore = useFriendsFilterStore();
  const themeStore = useThemeStore();
  const onboardingStore = useOnboardingStore();

  profilesStore.clearLocal();
  accountsStore.clearLocal();
  customLinksStore.clearLocal();
  friendsStore.clearLocal();
  themeStore.resetLocalPreferences();
  onboardingStore.completed = false;

  localStorage.removeItem("user-locale");
  localStorage.removeItem("theme");
  localStorage.removeItem("grayscale");
  localStorage.removeItem("sfz_haptic");
  localStorage.removeItem("sfz_tap_sound");
  localStorage.removeItem("sfz_tap_sound_variant");
  localStorage.removeItem("sfz_text_zoom");
  clearCloudSyncQueue();
}

function applyCloudSnapshot(snapshot: CloudSnapshot) {
  const profilesStore = useProfilesStore();
  const customLinksStore = useCustomLinksStore();
  const friendsStore = useFriendsFilterStore();
  const accountsStore = useAccountsStore();

  const settings = asCloudSettings(snapshot.settings);
  applyCloudSettings(settings);
  profilesStore.replaceFromCloud(snapshot.profiles, settings?.activeProfileId);
  customLinksStore.replaceFromCloud(snapshot.customLinks);
  friendsStore.replaceFromCloud(
    snapshot.friendsFilters,
    settings?.friendsFilterEnabled ?? false,
  );
  accountsStore.replaceFromCloud(snapshot.socialAccounts, snapshot.activeAccounts);
}

async function seedCloudFromLocalIfEmpty(snapshot: CloudSnapshot) {
  const profilesStore = useProfilesStore();
  const customLinksStore = useCustomLinksStore();
  const friendsStore = useFriendsFilterStore();
  const accountsStore = useAccountsStore();
  const themeStore = useThemeStore();
  const onboardingStore = useOnboardingStore();

  if (!snapshot.settings) {
    await syncSettingsPatch({
      theme: themeStore.themeMode as ThemeMode,
      language: localStorage.getItem("user-locale") ?? "fr",
      grayscaleEnabled: themeStore.grayscaleEnabled,
      textZoom: Number(localStorage.getItem("sfz_text_zoom") ?? "100"),
      hapticEnabled: localStorage.getItem("sfz_haptic") !== "false",
      tapSoundEnabled: localStorage.getItem("sfz_tap_sound") === "true",
      tapSoundVariant: normalizeTapSoundVariant(localStorage.getItem("sfz_tap_sound_variant")),
      activeProfileId: profilesStore.activeProfileId || undefined,
      onboardingCompleted: onboardingStore.completed,
      friendsFilterEnabled: friendsStore.enabled,
    });
  }

  if (snapshot.profiles.length === 0 && profilesStore.profiles.length > 0) {
    await profilesStore.seedCloud();
  }

  if (snapshot.customLinks.length === 0 && Object.keys(customLinksStore.links).length > 0) {
    await customLinksStore.seedCloud();
  }

  if (snapshot.friendsFilters.length === 0 && Object.keys(friendsStore.friends).length > 0) {
    await friendsStore.seedCloud();
  }

  if (snapshot.socialAccounts.length === 0 && accountsStore.accounts.length > 0) {
    await accountsStore.seedCloud();
  }
}

export async function hydrateCloudState(options?: {
  allowLocalSeedIfEmpty?: boolean;
}) {
  if (!isAuthenticated.value) return;
  if (hydratePromise) return hydratePromise;

  hydratePromise = (async () => {
    const client = getConvexClient();
    const user = await client.query(api.users.getMe, {});
    if (!user?._id) return;
    if (hydratedUserId === user._id) return;

    const rememberedUserId = getRememberedCloudUserId();
    const isAnonymousUser = user.isAnonymous === true;
    const canReuseLocalState = canReuseLocalCloudState({
      isAnonymousUser,
      rememberedUserId,
      currentUserId: user._id,
    });

    if (!canReuseLocalState) {
      clearCloudSyncQueue();
    }

    let snapshot = await fetchCloudSnapshot(client);

    await advancePostAuthSyncStage("dataReceived");

    const shouldKeepLocalIfCloudEmpty = shouldKeepLocalWhenCloudEmpty({
      canReuseLocalState,
      allowLocalSeedIfEmpty: options?.allowLocalSeedIfEmpty,
    });

    if (isCloudSnapshotEmpty(snapshot) && shouldKeepLocalIfCloudEmpty) {
      if (canReuseLocalState && hasPendingCloudSync()) {
        await flushCloudSyncQueue();
        snapshot = await fetchCloudSnapshot(client);
      }

      if (isCloudSnapshotEmpty(snapshot)) {
        await seedCloudFromLocalIfEmpty(snapshot);
      } else {
        clearCloudSyncQueue();
        applyCloudSnapshot(snapshot);
      }
    } else {
      // Cloud wins whenever it already has data. Dropping the local durable queue
      // here prevents stale pre-auth/Profile 1 writes from being replayed back
      // into Convex just before hydration.
      clearCloudSyncQueue();
      if (!canReuseLocalState) {
        clearCloudBackedLocalState();
      }
      applyCloudSnapshot(snapshot);
    }
    await advancePostAuthSyncStage("dataApplied");

    hydratedUserId = user._id;
    rememberCloudUserId(user._id);
  })().finally(() => {
    hydratePromise = null;
  });

  return hydratePromise;
}

export function resetCloudSyncState() {
  hydratedUserId = null;
  hydratePromise = null;
  resetPostAuthSyncFeedback();
}

export function resetSyncedLocalState() {
  clearCloudBackedLocalState();

  localStorage.removeItem("sfz_email");
  localStorage.removeItem("sf_jwt");
  localStorage.removeItem("sf_refresh");
  localStorage.removeItem("__convexAuthJWT");
  localStorage.removeItem("__convexAuthRefreshToken");
  clearRememberedCloudUserId();
}

export function consumeReopenSettingsAfterAuth() {
  const shouldReopen = localStorage.getItem(REOPEN_SETTINGS_AFTER_AUTH_KEY) === "1";
  if (shouldReopen) {
    localStorage.removeItem(REOPEN_SETTINGS_AFTER_AUTH_KEY);
  }
  return shouldReopen;
}

export async function finalizePasswordSignIn(options?: {
  email?: string;
  flow?: "signIn" | "signUp";
  reload?: boolean;
  reopenSettings?: boolean;
}) {
  beginPostAuthSyncFeedback();

  if (options?.email) {
    localStorage.setItem("sfz_email", options.email);
  }

  try {
    await hydrateCloudState({
      allowLocalSeedIfEmpty: options?.flow === "signUp",
    });

    if (options?.reload ?? true) {
      if (options?.reopenSettings) {
        localStorage.setItem(REOPEN_SETTINGS_AFTER_AUTH_KEY, "1");
      }
      await advancePostAuthSyncStage("restarting");
      queuePostAuthReadyNotice();
      window.setTimeout(() => {
        window.location.reload();
      }, AUTH_RELOAD_DELAY_MS);
      return;
    }

    showPostAuthReadyFeedback();
  } catch (error) {
    resetPostAuthSyncFeedback();
    throw error;
  }
}
