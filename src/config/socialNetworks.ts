export type BuiltInSocialNetwork = {
  id: string
  label: string
  route: `/${string}`
  url: string
  icon: string
  color: string
  tileColor?: string
  customIcon?: 'threads' | 'snapchat' | 'nextdoor'
  onboarding: boolean
  defaultSelected: boolean
}

export const builtInSocialNetworks: BuiltInSocialNetwork[] = [
  {
    id: 'twitter',
    label: 'Twitter / X',
    route: '/twitter',
    url: 'https://x.com',
    icon: 'pi pi-twitter',
    color: '#1DA1F2',
    tileColor: '#000000',
    onboarding: true,
    defaultSelected: true,
  },
  {
    id: 'facebook',
    label: 'Facebook',
    route: '/facebook',
    url: 'https://facebook.com',
    icon: 'pi pi-facebook',
    color: '#1877F2',
    onboarding: true,
    defaultSelected: true,
  },
  {
    id: 'instagram',
    label: 'Instagram',
    route: '/instagram',
    url: 'https://instagram.com',
    icon: 'pi pi-instagram',
    color: '#E4405F',
    tileColor: 'linear-gradient(135deg, #f09433, #e6683c, #dc2743, #cc2366, #bc1888)',
    onboarding: true,
    defaultSelected: true,
  },
  {
    id: 'linkedin',
    label: 'LinkedIn',
    route: '/linkedin',
    url: 'https://linkedin.com',
    icon: 'pi pi-linkedin',
    color: '#0A66C2',
    onboarding: true,
    defaultSelected: true,
  },
  {
    id: 'tiktok',
    label: 'TikTok',
    route: '/tiktok',
    url: 'https://tiktok.com',
    icon: 'pi pi-tiktok',
    color: '#000000',
    tileColor: '#010101',
    onboarding: true,
    defaultSelected: true,
  },
  {
    id: 'threads',
    label: 'Threads',
    route: '/threads',
    url: 'https://threads.net',
    icon: 'pi pi-at',
    customIcon: 'threads',
    color: '#000000',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'discord',
    label: 'Discord',
    route: '/discord',
    url: 'https://discord.com/app',
    icon: 'pi pi-discord',
    color: '#5865F2',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'reddit',
    label: 'Reddit',
    route: '/reddit',
    url: 'https://reddit.com',
    icon: 'pi pi-reddit',
    color: '#FF4500',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'snapchat',
    label: 'Snapchat',
    route: '/snapchat',
    url: 'https://web.snapchat.com',
    icon: 'pi pi-camera',
    customIcon: 'snapchat',
    color: '#FFFC00',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'cinderreels',
    label: 'CinderReels',
    route: '/cinderreels',
    url: 'https://cinderreels.com/',
    icon: 'pi pi-camera',
    color: '#E11D48',
    tileColor: '#E11D48',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'quora',
    label: 'Quora',
    route: '/quora',
    url: 'https://www.quora.com',
    icon: 'pi pi-question-circle',
    color: '#B92B27',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'pinterest',
    label: 'Pinterest',
    route: '/pinterest',
    url: 'https://www.pinterest.com',
    icon: 'pi pi-pinterest',
    color: '#BD081C',
    tileColor: '#E60023',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'telegram',
    label: 'Telegram',
    route: '/telegram',
    url: 'https://web.telegram.org',
    icon: 'pi pi-telegram',
    color: '#26A5E4',
    tileColor: '#0088cc',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'nextdoor',
    label: 'Nextdoor',
    route: '/nextdoor',
    url: 'https://nextdoor.com',
    icon: 'pi pi-map-marker',
    customIcon: 'nextdoor',
    color: '#8ED500',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'patreon',
    label: 'Patreon',
    route: '/patreon',
    url: 'https://www.patreon.com',
    icon: 'pi pi-heart',
    color: '#FF424D',
    tileColor: '#FF424D',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'theresanaiforthat',
    label: "There's An AI For That",
    route: '/theresanaiforthat',
    url: 'https://theresanaiforthat.com',
    icon: 'pi pi-sparkles',
    color: '#111827',
    tileColor: '#111827',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'industrysocial',
    label: 'Industry Social',
    route: '/industrysocial',
    url: 'https://industrysocial.net',
    icon: 'pi pi-building',
    color: '#2563EB',
    tileColor: '#2563EB',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'bluesky',
    label: 'Bluesky',
    route: '/bluesky',
    url: 'https://bsky.app',
    icon: 'pi pi-comments',
    color: '#1185FE',
    tileColor: '#1185FE',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'mastodon',
    label: 'Mastodon',
    route: '/mastodon',
    url: 'https://mastodon.social',
    icon: 'pi pi-globe',
    color: '#6364FF',
    tileColor: '#6364FF',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'substack',
    label: 'Substack',
    route: '/substack',
    url: 'https://substack.com',
    icon: 'pi pi-envelope',
    color: '#FF6719',
    tileColor: '#FF6719',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'ko-fi',
    label: 'Ko-fi',
    route: '/ko-fi',
    url: 'https://ko-fi.com',
    icon: 'pi pi-heart',
    color: '#29ABE0',
    tileColor: '#29ABE0',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'buymeacoffee',
    label: 'Buy Me a Coffee',
    route: '/buymeacoffee',
    url: 'https://www.buymeacoffee.com',
    icon: 'pi pi-heart',
    color: '#FFDD00',
    tileColor: '#FFDD00',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'producthunt',
    label: 'Product Hunt',
    route: '/producthunt',
    url: 'https://www.producthunt.com',
    icon: 'pi pi-megaphone',
    color: '#DA552F',
    tileColor: '#DA552F',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'indiehackers',
    label: 'Indie Hackers',
    route: '/indiehackers',
    url: 'https://www.indiehackers.com',
    icon: 'pi pi-users',
    color: '#0E2439',
    tileColor: '#0E2439',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'hackernews',
    label: 'Hacker News / Show HN',
    route: '/hackernews',
    url: 'https://news.ycombinator.com/show',
    icon: 'pi pi-bolt',
    color: '#FF6600',
    tileColor: '#FF6600',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'folloverse',
    label: 'Folloverse',
    route: '/folloverse',
    url: 'https://folloverse.com/?ref=betalist',
    icon: 'pi pi-users',
    color: '#7C3AED',
    tileColor: '#7C3AED',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'industrysocial-waitlist',
    label: 'Industry Social Waitlist',
    route: '/industrysocial-waitlist',
    url: 'https://industrysocial.net/waitlist',
    icon: 'pi pi-bookmark',
    color: '#1D4ED8',
    tileColor: '#1D4ED8',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'koru',
    label: 'Koru',
    route: '/koru',
    url: 'https://koru.now',
    icon: 'pi pi-link',
    color: '#16A34A',
    tileColor: '#16A34A',
    onboarding: true,
    defaultSelected: false,
  },
  {
    id: 'medium',
    label: 'Medium',
    route: '/medium',
    url: 'https://medium.com',
    icon: 'pi pi-bookmark',
    color: '#000000',
    tileColor: '#000000',
    onboarding: true,
    defaultSelected: false,
  },
]

const NETWORK_ISOLATION_NOT_COVERED = [
  'sessionStorage',
  'indexedDB',
  'cacheStorage',
  'serviceWorker',
  'httpCache',
  'credentialStore',
] as const

const NETWORK_ISOLATION_AUTH_STORAGE = ['cookies', 'localStorage'] as const

export type NetworkIsolationNotCovered = (typeof NETWORK_ISOLATION_NOT_COVERED)[number]
export type NetworkIsolationAuthStorage = (typeof NETWORK_ISOLATION_AUTH_STORAGE)[number]

export type NetworkIsolationPolicy = {
  authStorage: readonly NetworkIsolationAuthStorage[]
  storageOrigins: readonly string[]
  notCovered: readonly NetworkIsolationNotCovered[]
  notes?: string
}

type NetworkIsolationPolicyOverride = {
  authStorage?: readonly NetworkIsolationAuthStorage[]
  storageOrigins?: readonly string[]
  notCovered?: readonly NetworkIsolationNotCovered[]
  notes?: string
}

const NETWORK_ISOLATION_DEFAULT: NetworkIsolationPolicy = {
  authStorage: NETWORK_ISOLATION_AUTH_STORAGE,
  storageOrigins: [],
  notCovered: NETWORK_ISOLATION_NOT_COVERED,
}

const NETWORK_ISOLATION_OVERRIDES: Readonly<Record<string, NetworkIsolationPolicyOverride>> = {
  cinderreels: {
    authStorage: ['cookies', 'localStorage'],
    storageOrigins: ['https://cinderreels.com'],
  },
}

function normalizeHttpsOrigin(raw: string): string | null {
  try {
    const parsed = new URL(raw)
    if (parsed.protocol !== 'https:') return null
    const host = parsed.hostname.toLowerCase()
    if (!host) return null
    const isDefaultPort = !parsed.port || parsed.port === '443'
    return isDefaultPort ? `https://${host}` : `https://${host}:${parsed.port}`
  } catch {
    return null
  }
}

function uniqueOrigins(origins: readonly string[]): string[] {
  const deduped = new Set<string>()
  for (const origin of origins) {
    const normalized = normalizeHttpsOrigin(origin)
    if (normalized) deduped.add(normalized)
  }
  return Array.from(deduped)
}

export function getNetworkIsolationPolicy(networkId: string): NetworkIsolationPolicy {
  const override = NETWORK_ISOLATION_OVERRIDES[networkId]
  return {
    authStorage: override?.authStorage ?? NETWORK_ISOLATION_DEFAULT.authStorage,
    storageOrigins: uniqueOrigins(override?.storageOrigins ?? NETWORK_ISOLATION_DEFAULT.storageOrigins),
    notCovered: override?.notCovered ?? NETWORK_ISOLATION_DEFAULT.notCovered,
    notes: override?.notes,
  }
}

export function getNetworkIsolationOrigins(networkId: string): string[] {
  const networkUrl = builtInSocialNetworks.find((network) => network.id === networkId)?.url
  const baseOrigins = networkUrl ? [networkUrl] : []
  const policy = getNetworkIsolationPolicy(networkId)
  return uniqueOrigins([...baseOrigins, ...policy.storageOrigins])
}

export function getNetworkIsolationOriginsByNetwork(
  networkIds: readonly string[],
): Record<string, string[]> {
  return networkIds.reduce<Record<string, string[]>>((originsByNetwork, networkId) => {
    const origins = getNetworkIsolationOrigins(networkId)
    if (origins.length > 0) {
      originsByNetwork[networkId] = origins
    }
    return originsByNetwork
  }, {})
}
