<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { api } from "../api";
import type { Project } from "../types";
import ProjectExtras from "../components/ProjectExtras.vue";
const route = useRoute(),
  auth = useAuthStore(),
  projectId = computed(() => Number(route.params.id)),
  project = ref<Project>();
const canManageProject = computed(
  () => auth.isAdmin || project.value?.managerId === auth.user?.id,
);
onMounted(async () => {
  project.value = await api.project(projectId.value);
});
</script>
<template>
  <div class="page">
    <div class="page-header"><h1 class="page-title">项目文档与风险</h1></div>
    <ProjectExtras :project-id="projectId" :is-manager="canManageProject" />
  </div>
</template>
