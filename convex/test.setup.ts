// Include Convex runtime modules plus generated helpers required by convex-test.
export const modules = {
  "./_generated/api.js": () => import("./_generated/api.js"),
  "./_generated/server.js": () => import("./_generated/server.js"),
  "./socialAccounts.ts": () => import("./socialAccounts"),
  "./customLinks.ts": () => import("./customLinks"),
  "./settings.ts": () => import("./settings"),
  "./profiles.ts": () => import("./profiles"),
  "./friendsFilters.ts": () => import("./friendsFilters"),
  "./users.ts": () => import("./users"),
  "./billing.ts": () => import("./billing"),
  "./auth.ts": () => import("./auth"),
  "./http.ts": () => import("./http"),
};
