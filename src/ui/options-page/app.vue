<script setup lang="ts">
import { Notivue, Notification } from 'notivue'
</script>

<template>
  <div class="min-h-screen bg-base-100 text-base-content">
    <RouterView />
    <Notivue v-slot="item">
      <Notification :item="item" />
    </Notivue>
  </div>
</template>

<style scoped></style>
