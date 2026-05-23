---
title: "How SocialGlowz Keeps Android WebView Sessions in the Right Profile"
description: "A practical look at Android WebView session isolation in SocialGlowz: cookies, localStorage, origins, limits, and how to test profile boundaries."
date: "2026-05-23"
author: "SocialGlowz Team"
tags: ["android", "webview", "profiles", "security"]
---

Social work gets risky when account context is unclear. A creator can have a personal profile, a client profile, and a support profile open during the same day. A small team can move between brand accounts, inboxes, and publishing flows. If the app does not keep those contexts separate, a simple profile switch can become a session leak.

That is why SocialGlowz treats profile isolation as a core part of the Android experience, not as a cosmetic sidebar feature. On Android, embedded social networks run in native WebViews, and WebViews have their own storage behavior. Cookies matter, but they are not the whole story.

This article explains the practical model SocialGlowz uses today: what is isolated, why CinderReels needed explicit handling, how other networks are covered by default, and where the boundary of the guarantee stops.

## The Short Version

On Android, SocialGlowz isolates embedded WebView sessions by profile and network. The session boundary is the canonical key:

```text
${profileId}-${networkId}
```

That key is used to keep each profile/network pair separate. A CinderReels session in Profile A should not share the same Android WebView state as CinderReels in Profile B. The same principle applies to every embedded network.

The Android isolation layer covers two storage families:

- cookies persisted for the selected session;
- `localStorage` snapshots persisted by session and by exact web origin.

CinderReels has an explicit origin entry because its authentication relies on `localStorage`. Other networks still use the same isolation mechanism through the origin derived from their main URL. Extra origins are only needed when a network spreads authentication or app state across multiple domains.

## Why Cookies Alone Are Not Enough

Many web authentication flows rely on cookies. If cookies are separated correctly, switching from one profile to another can restore the right logged-in state.

But not every network keeps its auth state only in cookies. Some web apps also store account hints or session-adjacent state in `localStorage`. CinderReels is the reference case that made this explicit for SocialGlowz.

If an Android WebView keeps `localStorage` globally for a web origin, cookie isolation is not enough. Profile B might receive the right cookies while page JavaScript still reads stale `localStorage` from Profile A. That is exactly the class of problem the Android WebView storage isolation work addresses.

## The Session Boundary

SocialGlowz does not create a shared storage bucket for "CinderReels" or "Instagram" on Android. The boundary is always profile plus network:

```text
${profileId}-${networkId}
```

This matters because the same network can be used from different contexts:

- a personal creator profile;
- a client operations profile;
- a team support profile;
- a test profile used during QA.

Each of those contexts can open the same embedded network without intentionally sharing its session state with the others. When the user switches profiles, the Android layer restores the cookies and `localStorage` snapshots that belong to that profile/network pair.

## Why Origins Matter

Web storage is scoped by origin. An origin is the combination of scheme, host, and port, such as:

```text
https://cinderreels.com
```

SocialGlowz stores `localStorage` snapshots by exact origin instead of treating a network as one global string bag. That prevents values captured for one origin from being restored into another origin that should not receive them.

For most networks, the main URL gives SocialGlowz the primary origin. That is the default path: the network gets profile-aware cookies and `localStorage` isolation through its primary URL.

Some networks are more complex. They may send users through a separate authentication domain, an app subdomain, or another HTTPS origin during login. For those cases, SocialGlowz uses an additional origin matrix declared in `src/config/socialNetworks.ts`.

The rule is intentionally conservative: add only the origins that are observed and necessary for the real network flow. Do not add domains by guesswork.

## What CinderReels Changes

CinderReels does not get a special storage system. It uses the same global Android WebView isolation model as the other networks.

The difference is that CinderReels has an explicit `https://cinderreels.com` origin declaration because its authentication is known to use `localStorage`. That makes it a useful reference test for the feature:

1. open CinderReels in Profile A;
2. log in to Account A;
3. switch to Profile B;
4. confirm Account A is not visible;
5. log in to Account B;
6. switch back to Profile A;
7. confirm Account A returns without showing Account B.

That A/B/A scenario checks both directions: Profile B should not inherit Profile A, and Profile A should still restore its own state when you come back.

## What Happens During Navigation

The Android WebView layer prepares the active session before loading the target network. The goal is to restore the correct cookies and prepare the `localStorage` state early enough that the page does not start from the wrong profile.

When the Android WebView runtime supports the required features, SocialGlowz restores `localStorage` before page JavaScript runs and captures later `localStorage` changes through a validated bridge. Those captured values are stored under the same session key and exact origin.

When a required WebView feature is not available, SocialGlowz treats the result as degraded. Degraded mode should be visible in status or logs, and it must not be presented as complete `localStorage` isolation.

## What Is Covered

The Android WebView isolation model covers:

- per-session cookies;
- per-session `localStorage` snapshots;
- exact-origin restore and capture;
- network switching through the Android bottom bar;
- session delete behavior for the targeted profile/network pair;
- backup and restore payloads that keep `localStorage` under the same session and origin boundary.

The important idea is consistency. Opening, switching, deleting, backing up, and restoring should all respect the same profile/network boundary.

## What Is Not Covered

This is not a promise to isolate every possible browser storage mechanism.

The current Android WebView isolation contract does not cover:

- `IndexedDB`;
- `CacheStorage`;
- service workers;
- the global Android WebView HTTP cache;
- system credential stores.

`sessionStorage` should also not be treated as a durable isolation guarantee. If a network moves critical authentication state into one of the non-covered storage systems, that network needs fresh validation before SocialGlowz can claim the same profile-separation behavior for that state.

Clear limits are part of a reliable security model. It is better to name the boundary than to imply full browser isolation where the app does not control every layer.

## How To Add A Network Safely

For contributors, the network metadata starts in `src/config/socialNetworks.ts`.

When adding or reviewing a network:

1. start with the network's main HTTPS URL;
2. check whether login or app state moves through other HTTPS origins;
3. add only the observed extra origins that are needed;
4. keep the session boundary as `${profileId}-${networkId}`;
5. validate the A/B/A profile switch flow on Android;
6. document any degraded behavior or storage that is not covered.

Avoid network-specific branching unless there is a real reason. The goal is a shared isolation model with small declarative exceptions for origin coverage.

## How SocialGlowz Tests The Boundary

For Android, the practical validation path is the APK built by GitHub Actions on Blacksmith. In SocialGlowz development, the installable artifact is `socialglowz-android-debug`.

The strongest manual check is still the same A/B/A test:

- Profile A logs in to a network;
- Profile B opens the same network and must not see Profile A;
- Profile B logs in separately;
- Profile A opens again and must return to Profile A's own account.

CinderReels is the reference scenario because it exercises `localStorage`, not only cookies.

## The Principle

Profile isolation is not only about hiding UI tabs. It is about keeping web state attached to the context that created it.

For SocialGlowz, that means Android WebView sessions should stay inside their profile/network boundary, origins should be explicit when they need to be, and limits should be documented instead of hidden.

That gives users and contributors a clearer rule: sessions stay where they should, and any new network has to prove it follows the same boundary.
