import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";
import { authTables } from "@convex-dev/auth/server";

export default defineSchema({
  ...authTables,

  users: defineTable({
    email: v.optional(v.string()),
    emailVerificationTime: v.optional(v.number()),
    name: v.optional(v.string()),
    avatarUrl: v.optional(v.string()),
    isAnonymous: v.optional(v.boolean()),
    phone: v.optional(v.string()),
    phoneVerificationTime: v.optional(v.number()),
    createdAt: v.optional(v.number()),
  })
    .index("email", ["email"])
    .index("phone", ["phone"]),

  socialAccounts: defineTable({
    userId: v.id("users"),
    accountId: v.string(),
    networkId: v.string(),
    label: v.string(),
    addedAt: v.number(),
  })
    .index("by_userId", ["userId"])
    .index("by_user_network", ["userId", "networkId"])
    .index("by_accountId", ["accountId"]),

  activeAccounts: defineTable({
    userId: v.id("users"),
    networkId: v.string(),
    accountId: v.string(),
  })
    .index("by_userId", ["userId"])
    .index("by_user_network", ["userId", "networkId"]),

  settings: defineTable({
    userId: v.id("users"),
    theme: v.union(v.literal("light"), v.literal("dark"), v.literal("auto")),
    language: v.optional(v.string()),
    sidebarVisible: v.optional(v.boolean()),
    grayscaleEnabled: v.optional(v.boolean()),
    textZoom: v.optional(v.number()),
    hapticEnabled: v.optional(v.boolean()),
    tapSoundEnabled: v.optional(v.boolean()),
    tapSoundVariant: v.optional(v.union(v.literal("classic"), v.literal("soft"), v.literal("pop"))),
    activeProfileId: v.optional(v.string()),
    onboardingCompleted: v.optional(v.boolean()),
    friendsFilterEnabled: v.optional(v.boolean()),
  }).index("by_userId", ["userId"]),

  profiles: defineTable({
    userId: v.id("users"),
    profileId: v.string(),
    name: v.string(),
    emoji: v.string(),
    avatar: v.optional(v.string()),
    hiddenNetworks: v.optional(v.array(v.string())),
    createdAt: v.number(),
  })
    .index("by_userId", ["userId"])
    .index("by_user_profile", ["userId", "profileId"]),

  customLinks: defineTable({
    userId: v.id("users"),
    linkId: v.string(),
    profileId: v.string(),
    label: v.string(),
    url: v.string(),
    icon: v.string(),
  })
    .index("by_userId", ["userId"])
    .index("by_user_link", ["userId", "linkId"])
    .index("by_user_profile", ["userId", "profileId"]),

  friendsFilters: defineTable({
    userId: v.id("users"),
    networkId: v.string(),
    names: v.array(v.string()),
  })
    .index("by_userId", ["userId"])
    .index("by_user_network", ["userId", "networkId"]),

  entitlements: defineTable({
    userId: v.id("users"),
    productId: v.string(),
    planId: v.string(),
    status: v.union(
      v.literal("active"),
      v.literal("revoked"),
      v.literal("expired"),
      v.literal("refunded"),
    ),
    source: v.union(
      v.literal("appsumo"),
      v.literal("manual"),
      v.literal("lemon_squeezy"),
      v.literal("polar"),
      v.literal("stripe"),
      v.literal("paddle"),
    ),
    sourceEventId: v.optional(v.string()),
    externalCustomerId: v.optional(v.string()),
    externalOrderId: v.optional(v.string()),
    startsAt: v.number(),
    expiresAt: v.optional(v.number()),
    metadata: v.optional(v.any()),
    createdAt: v.number(),
    updatedAt: v.number(),
  })
    .index("by_user_product", ["userId", "productId"])
    .index("by_user_product_plan", ["userId", "productId", "planId"])
    .index("by_source_event", ["source", "sourceEventId"]),

  redemptionCodes: defineTable({
    code: v.string(),
    productId: v.string(),
    planId: v.string(),
    source: v.union(v.literal("appsumo"), v.literal("manual")),
    status: v.union(
      v.literal("available"),
      v.literal("redeemed"),
      v.literal("disabled"),
    ),
    createdAt: v.number(),
    updatedAt: v.number(),
    redeemedAt: v.optional(v.number()),
    redeemedBy: v.optional(v.id("users")),
    externalOrderId: v.optional(v.string()),
    note: v.optional(v.string()),
  })
    .index("by_code", ["code"])
    .index("by_redeemedBy", ["redeemedBy"]),

  billingEvents: defineTable({
    userId: v.optional(v.id("users")),
    productId: v.string(),
    planId: v.optional(v.string()),
    source: v.union(
      v.literal("appsumo"),
      v.literal("manual"),
      v.literal("lemon_squeezy"),
      v.literal("polar"),
      v.literal("stripe"),
      v.literal("paddle"),
    ),
    eventType: v.string(),
    sourceEventId: v.optional(v.string()),
    externalCustomerId: v.optional(v.string()),
    externalOrderId: v.optional(v.string()),
    redemptionCodeId: v.optional(v.id("redemptionCodes")),
    entitlementId: v.optional(v.id("entitlements")),
    payload: v.optional(v.any()),
    createdAt: v.number(),
  })
    .index("by_user", ["userId"])
    .index("by_source_event", ["source", "sourceEventId"])
    .index("by_product", ["productId"]),

  subscriptions: defineTable({
    userId: v.id("users"),
    plan: v.union(v.literal("free"), v.literal("pro"), v.literal("team")),
    status: v.union(
      v.literal("active"),
      v.literal("canceled"),
      v.literal("past_due"),
    ),
    expiresAt: v.optional(v.number()),
  }).index("by_userId", ["userId"]),
});
