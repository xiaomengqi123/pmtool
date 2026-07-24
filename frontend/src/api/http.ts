import axios from "axios";
import { ElMessage } from "element-plus";
import router from "../router";
import type { ApiResponse } from "../types";

const http = axios.create({ baseURL: "/api/v1", timeout: 30000 });
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("pmtool-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
http.interceptors.response.use(
  (response) => {
    if (response.config.responseType === "blob") return response;
    const body = response.data as ApiResponse<unknown>;
    if (body.code !== 0) return Promise.reject(new Error(body.message));
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("pmtool-token");
      router.push("/login");
    }
    ElMessage.error(
      error.response?.data?.message ?? error.message ?? "请求失败",
    );
    return Promise.reject(error);
  },
);
export default http;
