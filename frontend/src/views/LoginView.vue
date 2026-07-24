<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { ElMessage } from "element-plus";
const username = ref("admin"),
  password = ref(""),
  loading = ref(false),
  router = useRouter(),
  auth = useAuthStore();
async function submit() {
  loading.value = true;
  try {
    await auth.login(username.value, password.value);
    router.push("/dashboard");
  } catch {
    ElMessage.error("登录失败，请检查账号和密码");
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <main class="login">
    <section class="login-card">
      <h1>PMTool</h1>
      <p>企业项目管理系统</p>
      <el-form @submit.prevent="submit"
        ><el-form-item
          ><el-input
            v-model="username"
            placeholder="用户名"
            @keyup.enter="submit" /></el-form-item
        ><el-form-item
          ><el-input
            v-model="password"
            type="password"
            show-password
            placeholder="密码"
            @keyup.enter="submit" /></el-form-item
        ><el-button
          type="primary"
          style="width: 100%"
          :loading="loading"
          @click="submit"
          >登录</el-button
        ></el-form
      >
    </section>
  </main>
</template>
<style scoped>
.login {
  height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #006d5c, #009982);
}
.login-card {
  width: 380px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 50px #00463d66;
}
.login-card h1 {
  margin: 0;
  color: #009982;
}
.login-card p {
  color: #64748b;
  margin: 8px 0 28px;
}
</style>
