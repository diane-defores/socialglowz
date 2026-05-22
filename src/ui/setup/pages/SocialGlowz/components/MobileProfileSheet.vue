<template>
  <Teleport to="body">
    <Transition name="sheet">
      <div
        v-if="modelValue"
        class="sheet-overlay"
        @click.self="closeSheet"
      >
        <div
          ref="sheetRef"
          class="profile-sheet"
          :style="sheetStyle"
        >
          <div
            class="sheet-drag-zone"
            @pointerdown="onDragStart"
            @pointermove="onDragMove"
            @pointerup="onDragEnd"
            @pointercancel="onDragCancel"
          >
            <!-- Handle -->
            <div class="sheet-handle" />

            <!-- Header -->
            <div class="sheet-header">
              <span class="sheet-title">{{ $t('profiles.title') }}</span>
              <button
                class="sheet-close-btn"
                @click="closeSheet"
              >
                <i class="pi pi-times" />
              </button>
            </div>
          </div>

          <!-- Profile list -->
          <div class="sheet-profiles">
            <div
              v-for="profile in profilesStore.profiles"
              :key="profile.id"
              class="sheet-profile-row"
              :class="{ 'sheet-profile-row--active': profile.id === profilesStore.activeProfileId }"
            >
              <!-- Avatar (tap to select profile) -->
              <div
                class="sheet-avatar"
                @click="selectProfile(profile.id)"
              >
                <img
                  v-if="profile.avatar"
                  :src="profile.avatar"
                  class="sheet-avatar-img"
                />
                <span
                  v-else
                  class="sheet-avatar-emoji"
                >{{ profile.emoji }}</span>
                <i
                  v-if="profile.id === profilesStore.activeProfileId"
                  class="pi pi-check sheet-avatar-check"
                />
              </div>

              <!-- Name / inline edit -->
              <div
                class="sheet-profile-info"
                @click="selectProfile(profile.id)"
              >
                <input
                  v-if="editingId === profile.id"
                  :ref="el => { if (el) editInputRef = el as HTMLInputElement }"
                  v-model="editName"
                  class="name-edit-input"
                  @blur="saveEdit(profile.id)"
                  @keydown.enter="saveEdit(profile.id)"
                  @keydown.escape="cancelEdit"
                  @click.stop
                />
                <span
                  v-else
                  class="sheet-profile-name"
                >{{ profile.name }}</span>
                <span
                  v-if="profile.id === profilesStore.activeProfileId"
                  class="active-label"
                >{{ $t('profile.active_label') }}</span>
              </div>

              <!-- Actions -->
              <div class="sheet-profile-actions">
                <button
                  class="sheet-action"
                  :title="$t('profile.rename_action')"
                  @click.stop="startEdit(profile)"
                >
                  <i class="pi pi-pencil" />
                </button>
                <button
                  class="sheet-action"
                  :title="$t('profile.avatar_action')"
                  @click.stop="pickAvatar(profile.id)"
                >
                  <i class="pi pi-camera" />
                </button>
                <button
                  class="sheet-action"
                  :title="$t('profile.clear_cookies_action')"
                  @click.stop="clearCookiesProfileId = clearCookiesProfileId === profile.id ? null : profile.id"
                >
                  <i class="pi pi-eraser" />
                </button>
                <button
                  v-if="profilesStore.profiles.length > 1"
                  class="sheet-action sheet-action--danger"
                  :title="$t('common.delete')"
                  @click.stop="deleteProfile(profile.id)"
                >
                  <i class="pi pi-trash" />
                </button>
              </div>
            </div>
          </div>

          <!-- Clear network session data per profile. -->
          <div
            v-if="clearCookiesProfileId"
            class="clear-cookies-section"
          >
            <div class="clear-cookies-header">
              <i class="pi pi-trash" />
              <span>{{ $t('profile.clear_cookies_header', { name: profilesStore.profiles.find(p => p.id === clearCookiesProfileId)?.name }) }}</span>
              <button
                class="sheet-close-btn"
                style="margin-left:auto;"
                @click="clearCookiesProfileId = null"
              >
                <i class="pi pi-times" />
              </button>
            </div>
            <div class="clear-cookies-list">
              <button
                v-for="nw in webviewNetworks"
                :key="nw.id"
                class="clear-cookie-row"
                @click="clearNetworkCookies(nw.id)"
              >
                <ThreadsIcon
                  v-if="nw.id === 'threads'"
                  size="0.9rem"
                  class="clear-cookie-icon"
                />
                <SnapchatIcon
                  v-else-if="nw.id === 'snapchat'"
                  size="0.9rem"
                  class="clear-cookie-icon"
                />
                <NextdoorIcon
                  v-else-if="nw.id === 'nextdoor'"
                  size="0.9rem"
                  class="clear-cookie-icon"
                />
                <QuoraIcon
                  v-else-if="nw.id === 'quora'"
                  size="0.9rem"
                  class="clear-cookie-icon"
                />
                <i
                  v-else
                  :class="nw.icon"
                  class="clear-cookie-icon"
                />
                <span class="clear-cookie-label">{{ nw.label }}</span>
                <span
                  v-if="clearedNetworks[`${clearCookiesProfileId}:${nw.id}`]"
                  class="clear-cookie-done"
                >
                  <i class="pi pi-check" />
                </span>
                <i
                  v-else
                  class="pi pi-eraser clear-cookie-action"
                />
              </button>
            </div>
          </div>

          <!-- Add new profile -->
          <div class="sheet-footer">
            <div
              v-if="addingNew"
              class="add-profile-form"
            >
              <input
                :ref="el => { if (el) addInputRef = el as HTMLInputElement }"
                v-model="newProfileName"
                class="name-edit-input"
                placeholder="Nom du profil…"
                @keydown.enter="confirmAdd"
                @keydown.escape="cancelAdd"
              />
              <button
                class="add-confirm-btn"
                @click="confirmAdd"
              >
                <i class="pi pi-check" />
              </button>
              <button
                class="add-cancel-btn"
                @click="cancelAdd"
              >
                <i class="pi pi-times" />
              </button>
            </div>
            <button
              v-else
              class="add-profile-btn"
              @click="startAdd"
            >
              <i class="pi pi-plus" />
              <span>{{ $t('profile.add_new_button') }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Hidden file input for avatar upload -->
  <input
    ref="avatarFileInput"
    type="file"
    accept="image/*"
    style="display: none"
    @change="handleAvatarChange"
  />
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onUnmounted, watch } from 'vue'
import { useProfilesStore } from '@/stores/profiles'
import { useWebviewStore, WEBVIEW_URLS } from '@/stores/webviewState'
import { builtInSocialNetworks } from '@/config/socialNetworks'
import type { Profile } from '@/stores/profiles'
import ThreadsIcon from './icons/ThreadsIcon.vue'
import SnapchatIcon from './icons/SnapchatIcon.vue'
import NextdoorIcon from './icons/NextdoorIcon.vue'
import QuoraIcon from './icons/QuoraIcon.vue'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()

const profilesStore = useProfilesStore()
const webviewStore = useWebviewStore()

// ─── Rename state ─────────────────────────────────────────────
const editingId = ref<string | null>(null)
const editName = ref('')
const editInputRef = ref<HTMLInputElement | null>(null)

// ─── Add profile state ────────────────────────────────────────
const addingNew = ref(false)
const newProfileName = ref('')
const addInputRef = ref<HTMLInputElement | null>(null)

// ─── Avatar state ─────────────────────────────────────────────
const avatarFileInput = ref<HTMLInputElement | null>(null)
const pendingAvatarProfileId = ref<string | null>(null)

// ─── Clear network session per profile ────────────────────────
const clearCookiesProfileId = ref<string | null>(null)
const clearedNetworks = ref<Record<string, boolean>>({})

const webviewNetworks = computed(() => {
  const socialNetworkById = new Map(
    builtInSocialNetworks.map((network) => [network.id, network]),
  )

  return Object.keys(WEBVIEW_URLS).map(id => ({
    id,
    icon: socialNetworkById.get(id)?.icon ?? 'pi pi-globe',
    label: socialNetworkById.get(id)?.label ?? id,
  }))
})

function clearNetworkCookies(networkId: string) {
  const profileId = clearCookiesProfileId.value
  if (!profileId) return
  const key = `${profileId}:${networkId}`
  clearedNetworks.value[key] = true
  import('@tauri-apps/api/core').then(({ invoke }) => {
    const p = webviewStore.activeUrl
      ? invoke('close_webview', { profileId, networkId }).catch(() => {})
      : Promise.resolve()
    return p.then(() => invoke('delete_network_session', { profileId, networkId }))
  }).catch(e => {
    console.error('Failed to clear network session:', e)
  })
}

// ─── Sheet actions ────────────────────────────────────────────
function closeSheet() {
  emit('update:modelValue', false)
  clearCookiesProfileId.value = null
  clearedNetworks.value = {}
  cancelEdit()
  cancelAdd()
}

const sheetRef = ref<HTMLElement | null>(null)
const dragOffset = ref(0)
const isDragging = ref(false)
const activePointerId = ref<number | null>(null)
const dragTargetRef = ref<HTMLElement | null>(null)
const dragStartY = ref(0)
const dragStartTime = ref(0)
let dragResetTimer: number | null = null

const sheetStyle = computed(() => ({
  '--sheet-drag-offset': `${dragOffset.value}px`,
  transition: isDragging.value ? 'none' : 'transform 250ms ease',
}))

function clearDragResetTimer() {
  if (dragResetTimer !== null) {
    window.clearTimeout(dragResetTimer)
    dragResetTimer = null
  }
}

function scheduleDragReset() {
  clearDragResetTimer()
  dragResetTimer = window.setTimeout(() => {
    dragOffset.value = 0
    isDragging.value = false
    activePointerId.value = null
    dragTargetRef.value = null
  }, 250)
}

function getDismissThreshold() {
  const sheetHeight = sheetRef.value?.offsetHeight ?? window.innerHeight * 0.5
  return Math.min(140, Math.max(72, sheetHeight * 0.2))
}

function shouldIgnoreDragStart(target: EventTarget | null) {
  if (!(target instanceof Element)) return false
  return Boolean(target.closest('button, a, input, textarea, select, label, [role="button"], [data-no-sheet-drag]'))
}

function detachWindowDragListeners() {
  window.removeEventListener('pointermove', onWindowDragMove)
  window.removeEventListener('pointerup', onWindowDragEnd)
  window.removeEventListener('pointercancel', onWindowDragCancel)
}

function attachWindowDragListeners() {
  detachWindowDragListeners()
  window.addEventListener('pointermove', onWindowDragMove, { passive: false })
  window.addEventListener('pointerup', onWindowDragEnd)
  window.addEventListener('pointercancel', onWindowDragCancel)
}

function onDragStart(event: PointerEvent) {
  if (!props.modelValue || !event.isPrimary) return
  if (event.pointerType === 'mouse' && event.button !== 0) return
  if (shouldIgnoreDragStart(event.target)) return

  isDragging.value = true
  activePointerId.value = event.pointerId
  dragTargetRef.value = event.currentTarget as HTMLElement | null
  dragStartY.value = event.clientY
  dragStartTime.value = event.timeStamp || performance.now()
  dragOffset.value = 0
  clearDragResetTimer()
  attachWindowDragListeners()

  dragTargetRef.value?.setPointerCapture?.(event.pointerId)
}

function onDragMove(event: PointerEvent) {
  if (!isDragging.value || event.pointerId !== activePointerId.value) return

  const nextOffset = Math.max(0, event.clientY - dragStartY.value)
  dragOffset.value = nextOffset

  if (nextOffset > 0) {
    event.preventDefault()
  }
}

function finishDrag(event?: PointerEvent) {
  if (!isDragging.value) return
  if (event && event.pointerId !== activePointerId.value) return

  const pointerId = activePointerId.value
  if (pointerId !== null && dragTargetRef.value?.hasPointerCapture?.(pointerId)) {
    dragTargetRef.value.releasePointerCapture(pointerId)
  }

  const elapsed = Math.max(1, (event?.timeStamp || performance.now()) - dragStartTime.value)
  const velocity = dragOffset.value / elapsed
  const shouldClose = dragOffset.value >= getDismissThreshold() || velocity >= 0.6

  detachWindowDragListeners()
  isDragging.value = false
  activePointerId.value = null
  dragTargetRef.value = null

  if (shouldClose) {
    closeSheet()
    scheduleDragReset()
    return
  }

  dragOffset.value = 0
}

function onDragEnd(event: PointerEvent) {
  finishDrag(event)
}

function onDragCancel(event: PointerEvent) {
  finishDrag(event)
}

function onWindowDragMove(event: PointerEvent) {
  onDragMove(event)
}

function onWindowDragEnd(event: PointerEvent) {
  onDragEnd(event)
}

function onWindowDragCancel(event: PointerEvent) {
  onDragCancel(event)
}

function selectProfile(profileId: string) {
  profilesStore.setActive(profileId)
  closeSheet()
}

function deleteProfile(profileId: string) {
  profilesStore.remove(profileId)
}

// ─── Rename ───────────────────────────────────────────────────
function startEdit(profile: Profile) {
  editingId.value = profile.id
  editName.value = profile.name
  nextTick(() => editInputRef.value?.focus())
}

function saveEdit(profileId: string) {
  const trimmed = editName.value.trim()
  if (trimmed) profilesStore.rename(profileId, trimmed)
  editingId.value = null
}

function cancelEdit() {
  editingId.value = null
}

// ─── Add profile ──────────────────────────────────────────────
function startAdd() {
  addingNew.value = true
  newProfileName.value = ''
  nextTick(() => addInputRef.value?.focus())
}

function confirmAdd() {
  const trimmed = newProfileName.value.trim()
  if (trimmed) profilesStore.add(trimmed)
  addingNew.value = false
}

function cancelAdd() {
  addingNew.value = false
}

// ─── Avatar upload ────────────────────────────────────────────
function pickAvatar(profileId: string) {
  pendingAvatarProfileId.value = profileId
  avatarFileInput.value?.click()
}

function handleAvatarChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !pendingAvatarProfileId.value) return

  const reader = new FileReader()
  reader.onload = (e) => {
    const dataUrl = e.target?.result as string
    if (dataUrl && pendingAvatarProfileId.value) {
      profilesStore.setAvatar(pendingAvatarProfileId.value, dataUrl)
    }
    pendingAvatarProfileId.value = null
  }
  reader.readAsDataURL(file)
  ;(event.target as HTMLInputElement).value = ''
}

watch(() => props.modelValue, (open) => {
  clearDragResetTimer()
  detachWindowDragListeners()

  if (open) {
    dragOffset.value = 0
    isDragging.value = false
    activePointerId.value = null
    dragTargetRef.value = null
    return
  }

  scheduleDragReset()
})

onUnmounted(() => {
  detachWindowDragListeners()
  clearDragResetTimer()
})
</script>

<style scoped>
/* ─── Profile bottom sheet ───────────────────────────────────── */

.sheet-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.profile-sheet {
  width: 100%;
  background: var(--surface-card);
  border-radius: 20px 20px 0 0;
  padding-bottom: env(safe-area-inset-bottom, 16px);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transform: translateY(var(--sheet-drag-offset, 0px));
}

.sheet-handle {
  width: 2.5rem;
  height: 4px;
  background: var(--surface-border);
  border-radius: 2px;
  margin: 0.75rem auto 0;
  flex-shrink: 0;
}

.sheet-drag-zone {
  flex-shrink: 0;
  touch-action: none;
  user-select: none;
}

.sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.25rem 0.5rem;
  flex-shrink: 0;
}

.sheet-title {
  font-size: 1rem;
  font-weight: 700;
  color: var(--text-color);
}

.sheet-close-btn {
  background: var(--surface-ground);
  border: none;
  border-radius: 50%;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-color-secondary);
  font-size: 0.75rem;
  transition: background-color 0.12s;
}

.sheet-close-btn:active {
  background: var(--surface-hover);
}

/* Profile list */
.sheet-profiles {
  flex: 1;
  overflow-y: auto;
  padding: 0 0.75rem;
}

.sheet-profile-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.6rem 0.5rem;
  border-radius: 12px;
  transition: background-color 0.12s;
  margin-bottom: 0.25rem;
}

.sheet-profile-row:active {
  background: var(--surface-hover);
}

.sheet-profile-row--active {
  background: color-mix(in srgb, var(--primary-color) 8%, transparent);
}

.sheet-avatar {
  position: relative;
  width: 3rem;
  height: 3rem;
  border-radius: 50%;
  background: var(--surface-ground);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  cursor: pointer;
  overflow: hidden;
  font-size: 1.4rem;
}

.sheet-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sheet-avatar-emoji {
  font-size: 1.4rem;
  line-height: 1;
}

.sheet-avatar-check {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--primary-color) 75%, transparent);
  color: #fff;
  font-size: 0.9rem;
  border-radius: 50%;
}

.sheet-profile-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
  cursor: pointer;
  overflow: hidden;
}

.sheet-profile-name {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-color);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.active-label {
  font-size: 0.7rem;
  color: var(--primary-color);
  font-weight: 600;
}

.name-edit-input {
  flex: 1;
  font-size: 0.9rem;
  background: var(--surface-ground);
  border: 1.5px solid var(--primary-color);
  border-radius: 6px;
  padding: 0.3rem 0.5rem;
  color: var(--text-color);
  outline: none;
  width: 100%;
}

/* Action buttons */
.sheet-profile-actions {
  display: flex;
  gap: 0.15rem;
  flex-shrink: 0;
}

.sheet-action {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--text-color-secondary);
  padding: 0.4rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  font-size: 0.85rem;
  transition: color 0.12s, background-color 0.12s;
}

.sheet-action:active {
  background: var(--surface-hover);
  color: var(--text-color);
}

.sheet-action--danger:active {
  color: #ef4444;
}

/* Clear cookies section */
.clear-cookies-section {
  border-top: 1px solid var(--surface-border);
  padding: 0.5rem 0.75rem;
}

.clear-cookies-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-color);
  padding: 0.25rem 0.25rem 0.5rem;
}

.clear-cookies-header i:first-child {
  font-size: 0.85rem;
  color: var(--text-color-secondary);
}

.clear-cookies-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.35rem;
}

.clear-cookie-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.6rem;
  background: var(--surface-ground);
  border: 1px solid var(--surface-border);
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.12s;
}

.clear-cookie-row:active {
  background: var(--surface-hover);
}

.clear-cookie-icon {
  font-size: 0.9rem;
  color: var(--text-color-secondary);
}

.clear-cookie-label {
  flex: 1;
  font-size: 0.78rem;
  font-weight: 500;
  color: var(--text-color);
  text-align: left;
}

.clear-cookie-action {
  font-size: 0.75rem;
  color: var(--text-color-secondary);
}

.clear-cookie-done {
  color: #22c55e;
  font-size: 0.8rem;
}

/* Footer */
.sheet-footer {
  border-top: 1px solid var(--surface-border);
  padding: 0.6rem 0.75rem;
  flex-shrink: 0;
}

.add-profile-form {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.add-profile-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.7rem 0.75rem;
  background: none;
  border: 1.5px dashed var(--surface-border);
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9rem;
  color: var(--text-color-secondary);
  transition: background-color 0.12s, color 0.12s, border-color 0.12s;
}

.add-profile-btn:active {
  background: var(--surface-hover);
  color: var(--text-color);
  border-color: var(--primary-color);
}

.add-confirm-btn,
.add-cancel-btn {
  width: 2.2rem;
  height: 2.2rem;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  flex-shrink: 0;
}

.add-confirm-btn {
  background: var(--primary-color);
  color: #fff;
}

.add-cancel-btn {
  background: var(--surface-ground);
  color: var(--text-color-secondary);
}

/* ─── Sheet transition ───────────────────────────────────────── */

.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.25s ease;
}

.sheet-enter-active .profile-sheet,
.sheet-leave-active .profile-sheet {
  transition: transform 0.25s ease;
}

.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}

.sheet-enter-from .profile-sheet,
.sheet-leave-to .profile-sheet {
  transform: translateY(calc(100% + var(--sheet-drag-offset, 0px)));
}

@media (prefers-reduced-motion: reduce) {
  .sheet-enter-active,
  .sheet-leave-active,
  .sheet-enter-active .profile-sheet,
  .sheet-leave-active .profile-sheet {
    transition: none;
  }
}
</style>
