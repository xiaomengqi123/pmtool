import { defineStore } from 'pinia'
import { api } from '../api'
import type { User } from '../types'
export const useAuthStore = defineStore('auth', { state:()=>({user:null as User|null}), getters:{loggedIn:s=>!!s.user, isManager:s=>['ADMIN','PM'].includes(s.user?.roleCode ?? ''), isAdmin:s=>s.user?.roleCode==='ADMIN'}, actions:{async login(username:string,password:string){const r=await api.login(username,password);localStorage.setItem('pmtool-token',r.token);this.user=r.user},async load(){if(localStorage.getItem('pmtool-token'))this.user=await api.me()},logout(){localStorage.removeItem('pmtool-token');this.user=null}} })

