<script setup lang="ts">
const version = __VERSION__
const changelog = __CHANGELOG__
// const gitCommit = __GIT_COMMIT__
// const gitURL = __GITHUB_URL__
// const commitURL = `${gitURL}/commit/${gitCommit}`
</script>

<template>
  <div>
    <RouterLinkUp />

    <p>Version: {{ version }}</p>
    <pre class="prose changelog">{{ changelog }}</pre>
  </div>
</template>

<style lang="scss" scoped>
:deep(.changelog) {
  white-space: pre-wrap;

  input[type='checkbox'] {
    @apply checkbox checkbox-info disabled:opacity-100;
  }
  table {
    @apply table table-zebra;
  }
}
</style>
