const DEFAULT_SITE_URL = 'https://socialglowz.com'
const DEFAULT_APP_URL = 'https://github.com/dianedef/SocialGlowz/releases/latest'
const DEFAULT_EMAIL_DOMAIN = 'socialglowz.com'
const DEFAULT_WINFLOWZ_CHECKOUT_URL = 'https://winflowz.com'

function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '')
}

export const SITE_URL = stripTrailingSlash(
  import.meta.env.PUBLIC_SITE_URL ?? DEFAULT_SITE_URL
)

export const APP_URL = stripTrailingSlash(
  import.meta.env.PUBLIC_APP_URL ?? DEFAULT_APP_URL
)

export const EMAIL_DOMAIN = (
  import.meta.env.PUBLIC_EMAIL_DOMAIN ?? DEFAULT_EMAIL_DOMAIN
).trim()
export const WINFLOWZ_CHECKOUT_URL = stripTrailingSlash(
  import.meta.env.PUBLIC_WINFLOWZ_CHECKOUT_URL ?? DEFAULT_WINFLOWZ_CHECKOUT_URL
)

export function siteUrl(path = '/'): string {
  return new URL(path, `${SITE_URL}/`).toString()
}

export function appUrl(path = ''): string {
  if (!path || path === '/') return APP_URL
  return new URL(path.replace(/^\/+/, ''), `${APP_URL}/`).toString()
}

export function winflowzCheckoutUrl(
  offerId: string,
  options: { source?: string; sourceRef?: string; successUrl?: string; cancelUrl?: string } = {}
): string {
  const url = new URL('/api/commerce/checkout', `${WINFLOWZ_CHECKOUT_URL}/`)
  url.searchParams.set('offerId', offerId)
  if (options.source) url.searchParams.set('source', options.source)
  if (options.sourceRef) url.searchParams.set('sourceRef', options.sourceRef)
  if (options.successUrl) url.searchParams.set('successUrl', options.successUrl)
  if (options.cancelUrl) url.searchParams.set('cancelUrl', options.cancelUrl)
  return url.toString()
}

export function contactEmail(localPart: string): string {
  return `${localPart}@${EMAIL_DOMAIN}`
}
