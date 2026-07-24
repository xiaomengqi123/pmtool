<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "../stores/auth";
import ProjectExtras from "../components/ProjectExtras.vue";
const route = useRoute(),
  auth = useAuthStore(),
  projectId = computed(() => Number(route.params.id));
</script>
<template>
  <div class="page">
    <div class="page-header"><h1 class="page-title">项目文档与风险</h1></div>
    <ProjectExtras :project-id="projectId" :is-manager="auth.isManager" />
  </div>
</template>
