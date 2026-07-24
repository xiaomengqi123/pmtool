<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  Bell,
  Folder,
  Grid,
  List,
  User,
  UserFilled,
  Clock,
} from "@element-plus/icons-vue";
import { api } from "../api";
import { useAuthStore } from "../stores/auth";
const auth = useAuthStore(),
  router = useRouter(),
  unread = ref(0);
const userName = computed(() => auth.user?.displayName ?? "");
onMounted(async () => {
  unread.value = (await api.unread()).count;
});
function logout() {
  auth.logout();
  router.push("/login");
}
</script>
<template>
  <el-container style="min-height: 100vh"
    ><el-aside width="220px" class="aside"
      ><div class="brand">PMTool</div>
      <el-menu
        router
        :default-active="$route.path"
        background-color="#009982"
        text-color="#d6f2ee"
        active-text-color="#fff"
      >
        <el-menu-item index="/dashboard"
          ><el-icon><Grid /></el-icon>工作台</el-menu-item
        ><el-menu-item index="/customers" v-permission="['ADMIN', 'PM']"
          ><el-icon><UserFilled /></el-icon>客户管理</el-menu-item
        ><el-menu-item index="/projects"
          ><el-icon><Folder /></el-icon>项目管理</el-menu-item
        ><el-menu-item index="/tasks"
          ><el-icon><List /></el-icon>任务管理</el-menu-item
        ><el-menu-item index="/work-logs"
          ><el-icon><Clock /></el-icon>工时管理</el-menu-item
        ><el-menu-item index="/users" v-permission="['ADMIN']"
          ><el-icon><User /></el-icon>人员管理</el-menu-item
        ><el-menu-item index="/organization" v-permission="['ADMIN']"
          ><el-icon><User /></el-icon>组织与角色</el-menu-item
        ><el-menu-item index="/audit" v-permission="['ADMIN']"
          ><el-icon><List /></el-icon>操作审计</el-menu-item
        ><el-menu-item index="/notifications"
          ><el-badge :value="unread" :hidden="!unread"
            ><el-icon><Bell /></el-icon></el-badge
          >通知中心</el-menu-item
        >
      </el-menu></el-aside
    ><el-container
      ><el-header class="header"
        ><el-breadcrumb
          ><el-breadcrumb-item>PMTool</el-breadcrumb-item
          ><el-breadcrumb-item>{{
            $route.meta.title || "项目管理"
          }}</el-breadcrumb-item></el-breadcrumb
        ><el-dropdown
          ><span class="user">{{ userName }} · {{ auth.user?.roleCode }}</span
          ><template #dropdown
            ><el-dropdown-menu
              ><el-dropdown-item @click="router.push('/profile')"
                >个人中心</el-dropdown-item
              ><el-dropdown-item divided @click="logout"
                >退出登录</el-dropdown-item
              ></el-dropdown-menu
            ></template
          ></el-dropdown
        ></el-header
      ><el-main><RouterView /></el-main></el-container
  ></el-container>
</template>
<style scoped>
.aside {
  background: #009982;
}
.brand {
  height: 64px;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  padding: 0 28px;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
}
.user {
  cursor: pointer;
  color: #334155;
}
</style>
