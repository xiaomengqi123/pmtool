import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles.css'
import App from './App.vue'
import { createPinia } from 'pinia'
import router from './router'
import { permission } from './directives/permission'

createApp(App).use(createPinia()).use(router).use(ElementPlus).directive('permission', permission).mount('#app')

